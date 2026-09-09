package run.ikaros.operations.notification;

import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DefaultNotificationService implements NotificationService {
    private final NotificationRepository notifications;

    public DefaultNotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public Flux<NotificationView> list(UUID recipientId) {
        if (recipientId == null) return Flux.error(new IllegalArgumentException("通知接收人不能为空"));
        return notifications.findAllByRecipientIdOrderByCreatedAtDescIdDesc(recipientId).map(NotificationView::from);
    }

    @Override
    public Mono<NotificationEntity> create(NotificationEntity notification) {
        return notifications.findByEventId(notification.eventId()).switchIfEmpty(
            Mono.defer(() -> notifications.save(notification))
                .onErrorResume(DuplicateKeyException.class, ignored -> notifications.findByEventId(notification.eventId())));
    }
}
