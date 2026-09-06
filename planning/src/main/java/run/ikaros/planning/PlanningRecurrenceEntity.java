package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_recurrence")
public record PlanningRecurrenceEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("task_id") UUID taskId, String rule, PlanningRecurrenceMode mode,
    @Column("time_zone") String timeZone, @Column("next_run_at") Instant nextRunAt,
    boolean active, @Column("last_run_at") Instant lastRunAt, @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt, @Version Long version) {}
