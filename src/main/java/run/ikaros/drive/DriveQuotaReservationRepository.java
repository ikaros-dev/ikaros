package run.ikaros.drive;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Instant;
public interface DriveQuotaReservationRepository extends ReactiveCrudRepository<DriveQuotaReservationEntity, UUID> {
    Mono<DriveQuotaReservationEntity> findByDriveSpaceIdAndUploadSessionIdAndState(UUID spaceId, UUID uploadSessionId,
                                                                                     QuotaReservationState state);
    Flux<DriveQuotaReservationEntity> findAllByDriveSpaceIdAndStateAndExpiresAtLessThanEqual(UUID spaceId,
                                                                                              QuotaReservationState state,
                                                                                              Instant expiresAt);
}
