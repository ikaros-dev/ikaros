package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Instant;

public interface StorageRestoreOperationRepository extends ReactiveCrudRepository<StorageRestoreOperationEntity, UUID> {
    Mono<StorageRestoreOperationEntity> findByPlacementIdAndProviderRestoreClassAndRestoreGeneration(UUID placementId,
        String providerRestoreClass, long restoreGeneration);
    Flux<StorageRestoreOperationEntity> findAllByStatusAndRestoreExpiresAtBefore(StorageRestoreOperationStatus status,
        Instant now);
}
