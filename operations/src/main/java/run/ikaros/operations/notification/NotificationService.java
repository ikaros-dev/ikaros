package run.ikaros.operations.notification;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationService {
    Flux<NotificationView> list(UUID recipientId);
    Mono<NotificationEntity> create(NotificationEntity notification);
}
