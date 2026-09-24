package run.ikaros.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;

public interface OutboxDeliveryRepository extends ReactiveCrudRepository<OutboxDeliveryEntity, UUID> {
    @Modifying
    @Query("insert into event_delivery (consumer_id, event_id, status, next_attempt_at, created_at, updated_at) "
        + "values (:consumerId, :eventId, 'PENDING', :now, :now, :now) "
        + "on conflict (consumer_id, event_id) do nothing")
    Mono<Integer> insertIfAbsent(String consumerId, UUID eventId, Instant now);

    @Query("select * from event_delivery where consumer_id = :consumerId and event_id = :eventId for update")
    Mono<OutboxDeliveryEntity> lockByConsumerIdAndEventId(String consumerId, UUID eventId);

    @Modifying
    @Query("update event_delivery set status = 'DELIVERED', last_attempt_at = :now, next_attempt_at = :now, "
        + "last_error_classification = null, updated_at = :now where consumer_id = :consumerId and event_id = :eventId")
    Mono<Integer> markDelivered(String consumerId, UUID eventId, Instant now);

    @Modifying
    @Query("update event_delivery set attempt_count = attempt_count + 1, last_attempt_at = :now, updated_at = :now "
        + "where consumer_id = :consumerId and event_id = :eventId and status in ('PENDING', 'RETRY', 'DEAD')")
    Mono<Integer> recordAttempt(String consumerId, UUID eventId, Instant now);

    @Modifying
    @Query("update event_delivery set attempt_count = attempt_count + 1, "
        + "status = case when attempt_count + 1 >= :maxAttempts then 'DEAD' else 'RETRY' end, "
        + "next_attempt_at = :now + make_interval(secs => least(3600.0::double precision, "
        + "30.0::double precision * power(2.0::double precision, least(attempt_count, 7)::double precision))), "
        + "last_attempt_at = :now, last_error_classification = :errorClassification, updated_at = :now "
        + "where consumer_id = :consumerId and event_id = :eventId and status <> 'DELIVERED'")
    Mono<Integer> recordFailure(String consumerId, UUID eventId, Instant now, String errorClassification,
                                int maxAttempts);
}
