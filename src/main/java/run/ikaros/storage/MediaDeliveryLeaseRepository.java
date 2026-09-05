package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface MediaDeliveryLeaseRepository extends ReactiveCrudRepository<MediaDeliveryLeaseEntity, UUID> {
    Mono<MediaDeliveryLeaseEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    Mono<Boolean> existsByBindingId(UUID bindingId);
    Mono<Boolean> existsByBlobIdAndReleasedAtIsNullAndLeaseExpiresAtAfter(UUID blobId, Instant now);
    Flux<MediaDeliveryLeaseEntity> findAllByReleasedAtIsNullAndLeaseExpiresAtBefore(Instant now);
}
