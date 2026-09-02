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

@Service
public class InMemoryStorageProviderRegistry implements StorageProviderRegistry {
    private final Map<UUID, StorageProvider> providers = new ConcurrentHashMap<>();

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
    public Mono<StorageProvider> disable(UUID providerId) { return change(providerId, StorageProviderStatus.DISABLED); }

    @Override
    public Mono<StorageProvider> get(UUID providerId) {
        return Mono.justOrEmpty(providers.get(providerId)).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")));
    }

    @Override
    public Flux<StorageProvider> list() { return Flux.fromIterable(List.copyOf(providers.values())); }

    @Override
    public Mono<Void> requireWritable(UUID providerId) {
        return get(providerId).flatMap(provider -> provider.status() == StorageProviderStatus.ENABLED
            ? Mono.empty() : Mono.error(new ConflictException("Storage Provider 当前不可写入")));
    }

    @Override
    public Mono<Void> requireWritableByKey(String providerKey) {
        return providers.values().stream().filter(provider -> provider.providerKey().equals(providerKey)).findFirst()
            .map(provider -> requireWritable(provider.id()))
            .orElseGet(() -> Mono.error(new NotFoundException("Storage Provider 不存在")));
    }

    private Mono<StorageProvider> change(UUID id, StorageProviderStatus status) {
        return get(id).map(current -> {
            StorageProvider updated = new StorageProvider(current.id(), current.providerKey(), current.providerType(),
                current.tier(), status, current.secretReference(), current.metadata(), current.createdAt(), Instant.now());
            providers.replace(id, current, updated);
            return updated;
        });
    }
}
