package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface StorageRestoreRequestItemRepository extends ReactiveCrudRepository<StorageRestoreRequestItemEntity, UUID> {
    Mono<StorageRestoreRequestItemEntity> findByRequestIdAndPlacementId(UUID requestId, UUID placementId);
    Flux<StorageRestoreRequestItemEntity> findAllByRequestId(UUID requestId);
}
