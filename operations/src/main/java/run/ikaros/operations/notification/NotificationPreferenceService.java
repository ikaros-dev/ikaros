package run.ikaros.operations.notification;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface NotificationPreferenceService {
    Mono<NotificationPreferenceView> get(UUID recipientId);
    Mono<NotificationPreferenceView> update(UUID recipientId, NotificationPreferenceRequest request);
    Mono<Boolean> enabled(UUID recipientId, String terminalStatus);
}
