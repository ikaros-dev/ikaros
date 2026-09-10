package run.ikaros.event;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEventEntity, UUID> {
    Flux<OutboxEventEntity> findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc();

    @Query("select count(*) from event_outbox where dispatched_at is null")
    Mono<Long> countPending();

    @Query("select count(*) from event_outbox where dispatched_at is null and attempt_count > 0")
    Mono<Long> countAttemptedPending();

    @Query("select max(last_attempt_at) from event_outbox where last_attempt_at is not null")
    Mono<java.time.Instant> lastAttemptAt();

    @Modifying
    @Query("update event_outbox set attempt_count = attempt_count + 1, last_attempt_at = :attemptedAt "
        + "where id = :id and dispatched_at is null")
    Mono<Integer> recordAttempt(UUID id, java.time.Instant attemptedAt);

    @Modifying
    @Query("update event_outbox set dispatched_at = :dispatchedAt, attempt_count = attempt_count + 1, "
        + "last_attempt_at = :dispatchedAt where id = :id and dispatched_at is null")
    Mono<Integer> markDispatched(UUID id, java.time.Instant dispatchedAt);
}
