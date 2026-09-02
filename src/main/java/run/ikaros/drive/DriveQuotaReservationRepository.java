package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
public interface DriveQuotaReservationRepository extends ReactiveCrudRepository<DriveQuotaReservationEntity, UUID> {
    Mono<DriveQuotaReservationEntity> findByDriveSpaceIdAndUploadSessionId(UUID spaceId, UUID uploadSessionId);
}
