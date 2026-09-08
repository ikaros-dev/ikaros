package run.ikaros.event;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;

public interface InboxEntryRepository extends ReactiveCrudRepository<InboxEntryEntity, UUID> {
    Mono<Boolean> existsByConsumerIdAndEventId(String consumerId, UUID eventId);

    @Modifying
    @Query("insert into event_inbox (consumer_id, event_id, processed_at) values (:consumerId, :eventId, :processedAt) "
        + "on conflict (consumer_id, event_id) do nothing")
    Mono<Integer> insertIfAbsent(String consumerId, UUID eventId, java.time.Instant processedAt);
}
