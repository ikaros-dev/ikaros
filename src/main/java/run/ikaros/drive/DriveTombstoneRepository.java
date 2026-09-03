package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface DriveTombstoneRepository extends ReactiveCrudRepository<DriveTombstoneEntity, UUID> {
    Flux<DriveTombstoneEntity> findAllByDriveSpaceIdAndSequenceGreaterThanOrderBySequenceAsc(UUID spaceId, long sequence);
}
