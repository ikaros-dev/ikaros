package run.ikaros.operations.notification;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DefaultNotificationPreferenceService implements NotificationPreferenceService {
    private final NotificationPreferenceRepository preferences;

    public DefaultNotificationPreferenceService(NotificationPreferenceRepository preferences) {
        this.preferences = preferences;
    }

    @Override
    public Mono<NotificationPreferenceView> get(UUID recipientId) {
        if (recipientId == null) return Mono.error(new IllegalArgumentException("通知接收人不能为空"));
        return preferences.findById(recipientId)
            .map(NotificationPreferenceView::from)
            .defaultIfEmpty(new NotificationPreferenceView(true, true));
    }

    @Override
    public Mono<NotificationPreferenceView> update(UUID recipientId, NotificationPreferenceRequest request) {
        if (recipientId == null || request == null) {
            return Mono.error(new IllegalArgumentException("通知偏好不能为空"));
        }
        return preferences.save(new NotificationPreferenceEntity(recipientId, request.taskSuccessEnabled(),
            request.taskFailureEnabled(), 0L, Instant.now())).map(NotificationPreferenceView::from);
    }

    @Override
    public Mono<Boolean> enabled(UUID recipientId, String terminalStatus) {
        return get(recipientId).map(value -> "SUCCEEDED".equals(terminalStatus)
            ? value.taskSuccessEnabled() : value.taskFailureEnabled());
    }
}
