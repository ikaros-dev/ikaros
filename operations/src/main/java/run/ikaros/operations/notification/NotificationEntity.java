package run.ikaros.operations.notification;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("notification")
public record NotificationEntity(
    @Id UUID id,
    @Column("event_id") UUID eventId,
    @Column("recipient_id") UUID recipientId,
    String source,
    @Column("event_type") String eventType,
    String title,
    String body,
    String priority,
    String status,
    @Column("task_id") UUID taskId,
    @Column("resource_id") UUID resourceId,
    @Column("created_at") Instant createdAt,
    @Column("read_at") Instant readAt,
    @Column("archived_at") Instant archivedAt,
    @Version Long version
) { }
