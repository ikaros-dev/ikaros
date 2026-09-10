package run.ikaros.storage;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.storage.api.UploadSessionState;

/** Upload session persistence boundary owned by Storage. */
public interface UploadSessionRepository extends ReactiveCrudRepository<UploadSessionEntity, UUID> {
    Mono<UploadSessionEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    Mono<UploadSessionEntity> findByOwnerIdAndResourceIdAndIdempotencyKey(UUID ownerId, UUID resourceId,
                                                                           String idempotencyKey);

    Flux<UploadSessionEntity> findAllByStateInAndExpiresAtBefore(Collection<UploadSessionState> states,
                                                                  java.time.Instant expiresAt);
}
