package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_time_entry")
public record PlanningTimeEntryEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("task_id") UUID taskId, @Column("duration_minutes") int durationMinutes,
    @Column("started_at") Instant startedAt, @Column("ended_at") Instant endedAt,
    PlanningTimeEntrySource source, String note, @Column("created_at") Instant createdAt) {}
