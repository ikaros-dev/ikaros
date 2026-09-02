package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface StorageRestoreOperationRepository extends ReactiveCrudRepository<StorageRestoreOperationEntity, UUID> {
    Mono<StorageRestoreOperationEntity> findByPlacementIdAndProviderRestoreClassAndRestoreGeneration(UUID placementId,
        String providerRestoreClass, long restoreGeneration);
}
