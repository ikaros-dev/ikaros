package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_focus_session")
public record PlanningFocusSessionEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("task_id") UUID taskId, PlanningFocusMode mode, PlanningFocusSessionStatus status,
    @Column("planned_minutes") Integer plannedMinutes, @Column("actual_minutes") Integer actualMinutes,
    @Column("started_at") Instant startedAt, @Column("ended_at") Instant endedAt, String note,
    @Column("created_at") Instant createdAt, @Version Long version) {}
