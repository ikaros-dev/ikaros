package run.ikaros.task;

import java.time.Instant;
import java.util.UUID;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("background_task")
public record BackgroundTaskEntity(@Id UUID id, @Column("task_type") String taskType, String status,
    @Column("payload") Json payload, @Column("idempotency_key") String idempotencyKey,
    @Column("available_at") Instant availableAt, @Column("timeout_at") Instant timeoutAt,
    @Column("lease_owner") String leaseOwner,
    @Column("lease_token") UUID leaseToken, @Column("lease_expires_at") Instant leaseExpiresAt,
    int attempt, @Column("cancel_requested_at") Instant cancelRequestedAt,
    @Column("progress") Json progress, @Column("result_summary") Json result,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt,
    @Column("parent_task_id") UUID parentTaskId) {
    public BackgroundTaskEntity(UUID id, String taskType, String status, String payload, String idempotencyKey,
        Instant availableAt, Instant timeoutAt, String leaseOwner, UUID leaseToken, Instant leaseExpiresAt,
        int attempt, Instant cancelRequestedAt, String progress, String result, Instant createdAt,
        Instant updatedAt, UUID parentTaskId) {
        this(id, taskType, status, Json.of(payload == null ? "{}" : payload), idempotencyKey, availableAt, timeoutAt,
            leaseOwner, leaseToken, leaseExpiresAt, attempt, cancelRequestedAt,
            Json.of(progress == null ? "{}" : progress), Json.of(result == null ? "{}" : result), createdAt, updatedAt,
            parentTaskId);
    }
}
