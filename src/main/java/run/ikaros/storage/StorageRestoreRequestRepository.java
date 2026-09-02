package run.ikaros.storage;

import java.util.UUID;
import java.util.Collection;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StorageRestoreRequestRepository extends ReactiveCrudRepository<StorageRestoreRequestEntity, UUID> {
    Mono<StorageRestoreRequestEntity> findByActorIdAndScopeAndScopeIdAndIdempotencyKey(UUID actorId,
        StorageRestoreScope scope, UUID scopeId, String idempotencyKey);
    Flux<StorageRestoreRequestEntity> findAllByActorIdOrderByCreatedAtDesc(UUID actorId);
    Mono<StorageRestoreRequestEntity> findFirstByActorIdAndScopeAndScopeIdAndStatusInOrderByCreatedAtDesc(
        UUID actorId, StorageRestoreScope scope, UUID scopeId, Collection<StorageRestoreRequestStatus> statuses);
}
