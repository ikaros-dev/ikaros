package run.ikaros.operations.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationView(
    UUID id, UUID eventId, UUID recipientId, String source, String eventType, String title, String body,
    String preview, String priority, String status, UUID taskId, UUID resourceId, Instant createdAt, Instant readAt,
    Instant archivedAt, String channel
) {
    static NotificationView from(NotificationEntity value) {
        return new NotificationView(value.id(), value.eventId(), value.recipientId(), value.source(),
            value.eventType(), value.title(), value.body(), value.body(), value.priority(), value.status(), value.taskId(),
            value.resourceId(), value.createdAt(), value.readAt(), value.archivedAt(), "IN_APP");
    }
}
