package run.ikaros.event;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEventEntity, UUID> {
    Flux<OutboxEventEntity> findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc();

    @Modifying
    @Query("update event_outbox set attempt_count = attempt_count + 1, last_attempt_at = :attemptedAt "
        + "where id = :id and dispatched_at is null")
    Mono<Integer> recordAttempt(UUID id, java.time.Instant attemptedAt);

    @Modifying
    @Query("update event_outbox set dispatched_at = :dispatchedAt, attempt_count = attempt_count + 1, "
        + "last_attempt_at = :dispatchedAt where id = :id and dispatched_at is null")
    Mono<Integer> markDispatched(UUID id, java.time.Instant dispatchedAt);
}
