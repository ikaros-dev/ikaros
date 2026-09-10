package run.ikaros.operations.notification;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("notification_preference")
public record NotificationPreferenceEntity(
    @Id @Column("recipient_id") UUID recipientId,
    @Column("task_success_enabled") boolean taskSuccessEnabled,
    @Column("task_failure_enabled") boolean taskFailureEnabled,
    @Version Long version,
    @Column("updated_at") Instant updatedAt
) { }
