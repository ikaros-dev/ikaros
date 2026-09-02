package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MediaDeliveryLeaseRepository extends ReactiveCrudRepository<MediaDeliveryLeaseEntity, UUID> {
    Mono<MediaDeliveryLeaseEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    Mono<Boolean> existsByBlobIdAndReleasedAtIsNullAndLeaseExpiresAtAfter(UUID blobId, Instant now);
}
