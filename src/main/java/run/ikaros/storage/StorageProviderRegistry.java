package run.ikaros.storage;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface StorageProviderRegistry {
    Mono<StorageProvider> register(String providerKey, String providerType, StorageTier tier,
                                    String secretReference, Map<String, Object> metadata);
    Mono<StorageProvider> enable(UUID providerId);
    Mono<StorageProvider> disable(UUID providerId);
    Mono<StorageProvider> get(UUID providerId);
    Collection<StorageProvider> list();
    Mono<Void> requireWritable(UUID providerId);
}
