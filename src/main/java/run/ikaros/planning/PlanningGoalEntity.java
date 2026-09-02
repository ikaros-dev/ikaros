package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_goal")
public record PlanningGoalEntity(@Id UUID id, @Column("owner_id") UUID ownerId, String title,
    String description, PlanningGoalType type, PlanningGoalStatus status, Double progress,
    @Column("start_at") Instant startAt, Instant deadline, @Column("completed_at") Instant completedAt,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
