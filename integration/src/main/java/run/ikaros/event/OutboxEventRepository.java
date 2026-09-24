package run.ikaros.event;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEventEntity, UUID> {
    Flux<OutboxEventEntity> findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc();

    @Query("select e.* from event_outbox e left join event_delivery d "
        + "on d.event_id = e.id and d.consumer_id = :consumerId "
        + "where d.id is null or d.status <> 'DELIVERED' "
        + "order by e.occurred_at asc, e.id asc limit 100")
    Flux<OutboxEventEntity> findTop100UndeliveredForConsumer(String consumerId);

    @Query("select e.* from event_outbox e left join event_delivery d "
        + "on d.event_id = e.id and d.consumer_id = :consumerId "
        + "where d.id is null or (d.status in ('PENDING', 'RETRY') and d.next_attempt_at <= :now) "
        + "order by e.occurred_at asc, e.id asc limit 100")
    Flux<OutboxEventEntity> findTop100PendingForConsumer(String consumerId, java.time.Instant now);

    @Query("select count(*) from event_outbox e "
        + "where not exists (select 1 from event_delivery d where d.event_id = e.id) "
        + "or exists (select 1 from event_delivery d where d.event_id = e.id and d.status <> 'DELIVERED')")
    Mono<Long> countPending();

    @Query("select count(*) from event_outbox e where exists "
        + "(select 1 from event_delivery d where d.event_id = e.id and d.attempt_count > 0 and d.status <> 'DELIVERED')")
    Mono<Long> countAttemptedPending();

    @Query("select max(last_attempt_at) from "
        + "(select last_attempt_at from event_outbox union all select last_attempt_at from event_delivery) attempts")
    Mono<java.time.Instant> lastAttemptAt();

    @Modifying
    @Query("update event_outbox set attempt_count = attempt_count + 1, last_attempt_at = :attemptedAt "
        + "where id = :id")
    Mono<Integer> recordAttempt(UUID id, java.time.Instant attemptedAt);

    @Modifying
    @Query("update event_outbox set dispatched_at = :dispatchedAt, attempt_count = attempt_count + 1, "
        + "last_attempt_at = :dispatchedAt where id = :id and dispatched_at is null")
    Mono<Integer> markDispatched(UUID id, java.time.Instant dispatchedAt);
}
