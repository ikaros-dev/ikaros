package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface DriveChangeRepository extends ReactiveCrudRepository<DriveChangeEntity, UUID> {
    Flux<DriveChangeEntity> findAllByDriveSpaceIdAndSequenceGreaterThanOrderBySequenceAsc(UUID spaceId, long sequence);
}
