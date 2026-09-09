package run.ikaros.operations.task;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface BackgroundTaskRepository extends ReactiveCrudRepository<BackgroundTaskEntity, UUID> {
    @org.springframework.data.r2dbc.repository.Query("select count(*) from background_task where status = :status")
    Mono<Long> countByStatus(String status);
    Flux<BackgroundTaskEntity> findAllByOrderByCreatedAtDesc();
    Flux<BackgroundTaskEntity> findAllByStatusOrderByCreatedAtDesc(String status);
    Mono<BackgroundTaskEntity> findByTaskTypeAndIdempotencyKey(String taskType, String idempotencyKey);
    Mono<BackgroundTaskEntity> findTop1ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscCreatedAtAsc(
        String status, Instant now);
    Mono<BackgroundTaskEntity> findTop1ByStatusAndLeaseExpiresAtLessThanEqualOrderByLeaseExpiresAtAsc(
        String status, Instant now);
    reactor.core.publisher.Flux<BackgroundTaskEntity> findAllByStatusAndTimeoutAtLessThanEqual(String status, Instant now);
}
