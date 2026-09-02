package run.ikaros.task;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("background_task_attempt")
public record BackgroundTaskAttemptEntity(@Id UUID id, @Column("task_id") UUID taskId,
    @Column("attempt_no") int attemptNo, String status, @Column("claimed_by") String claimedBy,
    @Column("lease_expires_at") Instant leaseExpiresAt, @Column("last_heartbeat_at") Instant heartbeatAt,
    @Column("started_at") Instant startedAt, @Column("ended_at") Instant endedAt,
    @Column("error_summary") String errorSummary, @Column("created_at") Instant createdAt) {
}
