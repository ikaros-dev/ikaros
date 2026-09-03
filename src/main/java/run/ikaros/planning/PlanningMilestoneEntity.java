package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_milestone")
public record PlanningMilestoneEntity(@Id UUID id, @Column("owner_id") UUID ownerId, String title,
    String description, @Column("goal_id") UUID goalId, @Column("project_id") UUID projectId,
    @Column("due_at") Instant dueAt, PlanningMilestoneStatus status, @Column("achieved_at") Instant achievedAt,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
