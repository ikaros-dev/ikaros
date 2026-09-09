package run.ikaros.operations.notification;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepository extends ReactiveCrudRepository<NotificationEntity, UUID> {
    Mono<NotificationEntity> findByEventId(UUID eventId);
    Flux<NotificationEntity> findAllByRecipientIdOrderByCreatedAtDescIdDesc(UUID recipientId);
}
