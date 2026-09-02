package run.ikaros.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** PostgreSQL-backed provider registry; secrets remain references, never plaintext. */
@Primary
@Service
public class PersistentStorageProviderRegistry implements StorageProviderRegistry {
    private final StorageProviderRepository repository;
    private final ObjectMapper mapper;

    @Autowired
    public PersistentStorageProviderRegistry(StorageProviderRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
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
                    StorageProviderStatus.ENABLED.name(), secretReference, json, now, now)).map(this::toModel);
            })));
    }

    @Override
    public Mono<StorageProvider> enable(UUID providerId) { return change(providerId, StorageProviderStatus.ENABLED); }

    @Override
    public Mono<StorageProvider> disable(UUID providerId) { return change(providerId, StorageProviderStatus.DISABLED); }

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
    public Flux<StorageProvider> list() { return repository.findAll().map(this::toModel); }

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
            .flatMap(repository::save).map(this::toModel);
    }

    private Mono<Void> checkWritable(StorageProvider provider) {
        return provider.status() == StorageProviderStatus.ENABLED ? Mono.empty()
            : Mono.error(new ConflictException("Storage Provider 当前不可写入"));
    }

    private Mono<String> encode(Map<String, Object> metadata) {
        try {
            return Mono.just(mapper.writeValueAsString(metadata == null ? Map.of() : metadata));
        } catch (JsonProcessingException error) {
            return Mono.error(new IllegalArgumentException("Provider metadata 无法序列化", error));
        }
    }

    private StorageProvider toModel(StorageProviderEntity entity) {
        try {
            Map<String, Object> metadata = mapper.readValue(entity.providerMetadata(),
                mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            return new StorageProvider(entity.id(), entity.providerKey(), entity.providerType(),
                StorageTier.valueOf(entity.tier()), StorageProviderStatus.valueOf(entity.status()),
                entity.secretReference(), metadata, entity.createdAt(), entity.updatedAt());
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw new ConflictException("Storage Provider 数据损坏");
        }
    }
}
