package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface DriveSpaceRepository extends ReactiveCrudRepository<DriveSpaceEntity, UUID> {
    Flux<DriveSpaceEntity> findAllByOwnerUserIdOrderByCreatedAtAsc(UUID ownerUserId);
}
