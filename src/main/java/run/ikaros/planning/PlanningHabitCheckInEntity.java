package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_habit_check_in")
public record PlanningHabitCheckInEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("habit_id") UUID habitId, Double value, @Column("occurred_at") Instant occurredAt,
    String note, @Column("created_at") Instant createdAt) {}
