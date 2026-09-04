package run.ikaros.storage;

import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StorageProviderRegistry {
    Mono<StorageProvider> register(String providerKey, String providerType, StorageTier tier,
                                    String secretReference, Map<String, Object> metadata);

    default Mono<StorageProvider> register(String providerKey, String providerType, StorageTier tier,
                                           String secretReference, Map<String, Object> metadata,
                                           String accessKeyId, String secretAccessKey, String sessionToken) {
        return register(providerKey, providerType, tier, secretReference, metadata);
    }
    Mono<StorageProvider> update(UUID providerId, UpdateStorageProviderRequest request);
    Mono<StorageProvider> enable(UUID providerId);
    Mono<StorageProvider> disable(UUID providerId);
    Mono<StorageProvider> drain(UUID providerId);
    Mono<StorageProvider> get(UUID providerId);
    Mono<StorageProvider> getByKey(String providerKey);
    Flux<StorageProvider> list();
    Mono<Void> requireWritable(UUID providerId);
    Mono<StorageProvider> requireWritableByKey(String providerKey);
}
