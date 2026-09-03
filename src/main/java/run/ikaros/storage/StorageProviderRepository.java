package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface StorageProviderRepository extends ReactiveCrudRepository<StorageProviderEntity, UUID> {
    Mono<StorageProviderEntity> findByProviderKey(String providerKey);
}
