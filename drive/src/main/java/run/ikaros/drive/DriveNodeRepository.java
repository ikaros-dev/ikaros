package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface DriveNodeRepository extends ReactiveCrudRepository<DriveNodeEntity, UUID> {
    Flux<DriveNodeEntity> findAllByDriveSpaceIdAndParentIdAndLifecycleOrderByNormalizedNameAsc(UUID spaceId, UUID parentId, DriveLifecycle lifecycle);
    Flux<DriveNodeEntity> findAllByDriveSpaceIdAndParentIdAndLifecycleOrderByNormalizedNameAsc(UUID spaceId, UUID parentId, DriveLifecycle lifecycle, Pageable pageable);
    Mono<Long> countByDriveSpaceIdAndParentIdAndLifecycle(UUID spaceId, UUID parentId, DriveLifecycle lifecycle);
    Mono<DriveNodeEntity> findByIdAndDriveSpaceId(UUID id, UUID spaceId);
}
