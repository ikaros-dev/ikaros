package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_habit")
public record PlanningHabitEntity(@Id UUID id, @Column("owner_id") UUID ownerId, String name, String description,
    PlanningHabitMetric metric, @Column("target_value") Double targetValue, String schedule,
    @Column("time_zone") String timeZone, @Column("start_at") Instant startAt, PlanningHabitStatus status,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
