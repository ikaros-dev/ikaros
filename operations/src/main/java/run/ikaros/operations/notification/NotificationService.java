package run.ikaros.operations.notification;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

public interface NotificationService {
    Flux<NotificationView> list(UUID recipientId);
    Mono<PageResponse<NotificationView>> search(UUID recipientId, String status, String source, String priority,
                                                int page, int size);
    Mono<NotificationEntity> create(NotificationEntity notification);
}
