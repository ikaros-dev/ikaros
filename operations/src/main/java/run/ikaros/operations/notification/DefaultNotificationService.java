package run.ikaros.operations.notification;

import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

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
    public Mono<PageResponse<NotificationView>> search(UUID recipientId, String status, String source,
                                                        String priority, int page, int size) {
        if (recipientId == null || page < 0 || size < 1 || size > 100) {
            return Mono.error(new IllegalArgumentException("通知分页参数不合法"));
        }
        String normalizedStatus = normalize(status);
        String normalizedSource = normalize(source);
        String normalizedPriority = normalize(priority);
        long offset = (long) page * size;
        return Mono.zip(notifications.search(recipientId, normalizedStatus, normalizedSource, normalizedPriority,
                size, offset).map(NotificationView::from).collectList(),
            notifications.countSearch(recipientId, normalizedStatus, normalizedSource, normalizedPriority))
            .map(result -> new PageResponse<>(result.getT1(), result.getT2(), page, size));
    }

    @Override
    public Mono<NotificationEntity> create(NotificationEntity notification) {
        return notifications.findByEventId(notification.eventId()).switchIfEmpty(
            Mono.defer(() -> notifications.save(notification))
                .onErrorResume(DuplicateKeyException.class, ignored -> notifications.findByEventId(notification.eventId())));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) return "";
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
