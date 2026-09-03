package run.ikaros.storage;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;

/** PostgreSQL-backed provider registry; secrets remain references, never plaintext. */
@Primary
@Service
public class PersistentStorageProviderRegistry implements StorageProviderRegistry {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final StorageProviderRepository repository;
    private final ObjectMapper mapper;
    private final DurableEventService events;

    @Autowired
    public PersistentStorageProviderRegistry(StorageProviderRepository repository, ObjectMapper mapper,
                                             DurableEventService events) {
        this.repository = repository;
        this.mapper = mapper;
        this.events = events;
    }

    @Override
    public Mono<StorageProvider> register(String providerKey, String providerType, StorageTier tier,
                                          String secretReference, Map<String, Object> metadata) {
        if (providerKey == null || providerKey.isBlank() || providerType == null || providerType.isBlank()
            || tier == null || secretReference == null || secretReference.isBlank()) {
            return Mono.error(new IllegalArgumentException("Storage Provider 参数不完整"));
        }
        if (!secretReference.startsWith("secret://")) {
            return Mono.error(new ConflictException("Provider secret reference 必须使用 secret:// URI"));
        }
        return repository.findByProviderKey(providerKey)
            .flatMap(existing -> Mono.<StorageProvider>error(new ConflictException("Storage Provider 标识已存在")))
            .switchIfEmpty(Mono.defer(() -> encode(metadata).flatMap(json -> {
                Instant now = Instant.now();
                return repository.save(new StorageProviderEntity(null, providerKey, providerType, tier.name(),
                    StorageProviderStatus.ENABLED.name(), secretReference, json, now, now)).map(this::toModel)
                    .flatMap(provider -> emit("storage.provider.created", provider,
                        "{\"provider_id\":\"" + provider.id() + "\",\"provider_type\":\"" + provider.providerType()
                            + "\",\"tier\":\"" + provider.tier() + "\"}").thenReturn(provider));
            })));
    }

    @Override
    public Mono<StorageProvider> enable(UUID providerId) { return change(providerId, StorageProviderStatus.ENABLED); }

    @Override
    public Mono<StorageProvider> update(UUID providerId, UpdateStorageProviderRequest request) {
        if (request == null) return Mono.error(new IllegalArgumentException("更新请求不能为空"));
        return repository.findById(providerId)
            .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .flatMap(current -> {
                String secret = request.secretReference() == null ? current.secretReference() : request.secretReference();
                if (request.secretReference() != null && !secret.startsWith("secret://")) {
                    return Mono.error(new ConflictException("Provider secret reference 必须使用 secret:// URI"));
                }
                String type = request.providerType() == null ? current.providerType() : request.providerType();
                String tier = request.tier() == null ? current.tier() : request.tier().name();
                Map<String, Object> requestedMetadata = request.metadata();
                if (requestedMetadata != null && requestedMetadata.values().stream().anyMatch(value -> value instanceof String valueString
                    && (valueString.toLowerCase().contains("password")
                        || valueString.toLowerCase().contains("secret")))) {
                    return Mono.error(new ConflictException("Provider metadata 不得保存明文凭据"));
                }
                return encode(requestedMetadata == null ? readMetadata(current.providerMetadata()) : requestedMetadata)
                    .flatMap(metadata -> repository.save(new StorageProviderEntity(current.id(), current.providerKey(),
                        type, tier, current.status(), secret, metadata, current.createdAt(), Instant.now())))
                    .map(this::toModel)
                    .flatMap(provider -> emit("storage.provider.updated", provider,
                        "{\"provider_id\":\"" + provider.id()
                            + "\",\"changed_fields\":" + changedFields(request) + "}")
                        .thenReturn(provider));
            });
    }

    @Override
    public Mono<StorageProvider> disable(UUID providerId) { return change(providerId, StorageProviderStatus.DISABLED); }

    @Override
    public Mono<StorageProvider> drain(UUID providerId) { return change(providerId, StorageProviderStatus.DRAINING); }

    @Override
    public Mono<StorageProvider> get(UUID providerId) {
        return repository.findById(providerId).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .map(this::toModel);
    }

    @Override
    public Mono<StorageProvider> getByKey(String providerKey) {
        return repository.findByProviderKey(providerKey)
            .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .map(this::toModel);
    }

    @Override
    public Flux<StorageProvider> list() { return repository.findAll().take(MAX_UNPAGED_RESULTS).map(this::toModel); }

    @Override
    public Mono<Void> requireWritable(UUID providerId) {
        return get(providerId).flatMap(this::checkWritable);
    }

    @Override
    public Mono<StorageProvider> requireWritableByKey(String providerKey) {
        return repository.findByProviderKey(providerKey)
            .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .map(this::toModel).flatMap(provider -> checkWritable(provider).thenReturn(provider));
    }

    private Mono<StorageProvider> change(UUID id, StorageProviderStatus status) {
        return repository.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .map(current -> new StorageProviderEntity(current.id(), current.providerKey(), current.providerType(),
                current.tier(), status.name(), current.secretReference(), current.providerMetadata(),
                current.createdAt(), Instant.now()))
            .flatMap(repository::save).map(this::toModel)
            .flatMap(provider -> {
                if (status == StorageProviderStatus.DRAINING) return Mono.just(provider);
                String event = status == StorageProviderStatus.ENABLED ? "storage.provider.enabled" : "storage.provider.disabled";
                return emit(event, provider, "{\"provider_id\":\"" + provider.id() + "\"}").thenReturn(provider);
            });
    }

    private Mono<Void> checkWritable(StorageProvider provider) {
        return provider.status() == StorageProviderStatus.ENABLED ? Mono.empty()
            : Mono.error(new ConflictException("Storage Provider 当前不可写入"));
    }

    private Mono<String> encode(Map<String, Object> metadata) {
        try {
            return Mono.just(mapper.writeValueAsString(metadata == null ? Map.of() : metadata));
        } catch (JacksonException error) {
            return Mono.error(new IllegalArgumentException("Provider metadata 无法序列化", error));
        }
    }

    private Map<String, Object> readMetadata(String json) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class));
        } catch (JacksonException error) {
            throw new ConflictException("Provider metadata 数据损坏");
        }
    }

    private String changedFields(UpdateStorageProviderRequest request) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        if (request.providerType() != null) fields.add("\"provider_type\"");
        if (request.tier() != null) fields.add("\"tier\"");
        if (request.secretReference() != null) fields.add("\"secret_reference\"");
        if (request.metadata() != null) fields.add("\"metadata\"");
        return "[" + String.join(",", fields) + "]";
    }

    private Mono<Void> emit(String eventType, StorageProvider provider, String payload) {
        return events.append(eventType, 1, "storage_provider", provider.id(), payload).then();
    }

    private StorageProvider toModel(StorageProviderEntity entity) {
        try {
            Map<String, Object> metadata = mapper.readValue(entity.providerMetadata(),
                mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            return new StorageProvider(entity.id(), entity.providerKey(), entity.providerType(),
                StorageTier.valueOf(entity.tier()), StorageProviderStatus.valueOf(entity.status()),
                entity.secretReference(), metadata, entity.createdAt(), entity.updatedAt());
        } catch (JacksonException | IllegalArgumentException error) {
            throw new ConflictException("Storage Provider 数据损坏");
        }
    }
}
