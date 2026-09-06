package run.ikaros.storage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;

@Service
public class InMemoryStorageProviderRegistry implements StorageProviderRegistry {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final Map<UUID, StorageProvider> providers = new ConcurrentHashMap<>();
    private final DurableEventService events;

    public InMemoryStorageProviderRegistry() {
        this(null);
    }

    public InMemoryStorageProviderRegistry(DurableEventService events) {
        this.events = events;
    }

    @Override
    public Mono<StorageProvider> register(String providerKey, String providerType, StorageTier tier,
                                          String secretReference, Map<String, Object> metadata) {
        if (providerKey == null || providerKey.isBlank() || providerType == null || providerType.isBlank()
            || tier == null || secretReference == null || secretReference.isBlank()) {
            return Mono.error(new IllegalArgumentException("Storage Provider 参数不完整"));
        }
        if (metadata != null && metadata.values().stream().anyMatch(value -> value instanceof String string
            && (string.toLowerCase().contains("password") || string.toLowerCase().contains("secret")))) {
            return Mono.error(new ConflictException("Provider metadata 不得保存明文凭据"));
        }
        return Mono.fromSupplier(() -> {
            if (providers.values().stream().anyMatch(provider -> provider.providerKey().equals(providerKey))) {
                throw new ConflictException("Storage Provider 标识已存在");
            }
            Instant now = Instant.now();
            StorageProvider provider = new StorageProvider(UUID.randomUUID(), providerKey, providerType, tier,
                StorageProviderStatus.ENABLED, secretReference, metadata, now, now);
            providers.put(provider.id(), provider);
            return provider;
        });
    }

    @Override
    public Mono<StorageProvider> enable(UUID providerId) { return change(providerId, StorageProviderStatus.ENABLED); }

    @Override
    public Mono<StorageProvider> update(UUID providerId, UpdateStorageProviderRequest request) {
        if (request == null) return Mono.error(new IllegalArgumentException("更新请求不能为空"));
        return get(providerId).flatMap(current -> {
            String secret = request.secretReference() == null ? current.secretReference() : request.secretReference();
            if (request.secretReference() != null && !secret.startsWith("secret://")) {
                return Mono.error(new ConflictException("Provider secret reference 必须使用 secret:// URI"));
            }
            Map<String, Object> metadata = request.metadata() == null ? current.metadata() : request.metadata();
            if (metadata.values().stream().anyMatch(value -> value instanceof String string
                && (string.toLowerCase().contains("password") || string.toLowerCase().contains("secret")))) {
                return Mono.error(new ConflictException("Provider metadata 不得保存明文凭据"));
            }
            StorageProvider updated = new StorageProvider(current.id(), current.providerKey(),
                request.providerType() == null ? current.providerType() : request.providerType(),
                request.tier() == null ? current.tier() : request.tier(), current.status(), secret, metadata,
                current.createdAt(), Instant.now());
            providers.replace(providerId, current, updated);
            return emitUpdated(updated, request).thenReturn(updated);
        });
    }

    @Override
    public Mono<StorageProvider> disable(UUID providerId) { return change(providerId, StorageProviderStatus.DISABLED); }

    @Override
    public Mono<StorageProvider> drain(UUID providerId) { return change(providerId, StorageProviderStatus.DRAINING); }

    @Override
    public Mono<StorageProvider> get(UUID providerId) {
        return Mono.justOrEmpty(providers.get(providerId)).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")));
    }

    @Override
    public Mono<StorageProvider> getByKey(String providerKey) {
        return providers.values().stream().filter(provider -> provider.providerKey().equals(providerKey)).findFirst()
            .map(Mono::just).orElseGet(() -> Mono.error(new NotFoundException("Storage Provider 不存在")));
    }

    @Override
    public Flux<StorageProvider> list() { return Flux.fromIterable(List.copyOf(providers.values())).take(MAX_UNPAGED_RESULTS); }

    @Override
    public Mono<Void> requireWritable(UUID providerId) {
        return get(providerId).flatMap(provider -> provider.status() == StorageProviderStatus.ENABLED
            ? Mono.empty() : Mono.error(new ConflictException("Storage Provider 当前不可写入")));
    }

    @Override
    public Mono<StorageProvider> requireWritableByKey(String providerKey) {
        return providers.values().stream().filter(provider -> provider.providerKey().equals(providerKey)).findFirst()
            .map(provider -> provider.status() == StorageProviderStatus.ENABLED ? Mono.just(provider)
                : Mono.<StorageProvider>error(new ConflictException("Storage Provider 当前不可写入")))
            .orElseGet(() -> Mono.<StorageProvider>error(new NotFoundException("Storage Provider 不存在")));
    }

    private Mono<StorageProvider> change(UUID id, StorageProviderStatus status) {
        return get(id).map(current -> {
            StorageProvider updated = new StorageProvider(current.id(), current.providerKey(), current.providerType(),
                current.tier(), status, current.secretReference(), current.metadata(), current.createdAt(), Instant.now());
            providers.replace(id, current, updated);
            return updated;
        });
    }

    private Mono<Void> emitUpdated(StorageProvider current, UpdateStorageProviderRequest request) {
        if (events == null) return Mono.empty();
        String payload = "{\"provider_id\":\"" + current.id() + "\",\"changed_fields\":"
            + changedFields(request) + "}";
        return events.append("storage.provider.updated", 1, "storage_provider", current.id(), payload).then();
    }

    private String changedFields(UpdateStorageProviderRequest request) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        if (request.providerType() != null) fields.add("\"provider_type\"");
        if (request.tier() != null) fields.add("\"tier\"");
        if (request.secretReference() != null) fields.add("\"secret_reference\"");
        if (request.metadata() != null) fields.add("\"metadata\"");
        return "[" + String.join(",", fields) + "]";
    }
}
