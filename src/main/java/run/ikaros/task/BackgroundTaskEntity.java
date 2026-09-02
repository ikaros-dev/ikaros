package run.ikaros.task;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("background_task")
public record BackgroundTaskEntity(@Id UUID id, @Column("task_type") String taskType, String status,
    @Column("payload") String payload, @Column("idempotency_key") String idempotencyKey,
    @Column("available_at") Instant availableAt, @Column("timeout_at") Instant timeoutAt,
    @Column("lease_owner") String leaseOwner,
    @Column("lease_token") UUID leaseToken, @Column("lease_expires_at") Instant leaseExpiresAt,
    int attempt, @Column("cancel_requested_at") Instant cancelRequestedAt,
    @Column("progress") String progress, @Column("result_summary") String result,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt,
    @Column("parent_task_id") UUID parentTaskId) {
}
