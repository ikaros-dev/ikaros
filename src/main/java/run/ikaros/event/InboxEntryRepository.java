package run.ikaros.event;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface InboxEntryRepository extends ReactiveCrudRepository<InboxEntryEntity, UUID> {
    Mono<Boolean> existsByConsumerIdAndEventId(String consumerId, UUID eventId);
}
