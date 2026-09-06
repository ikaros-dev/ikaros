package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_goal_task")
public record PlanningGoalTaskEntity(@Id UUID id, @Column("goal_id") UUID goalId,
    @Column("task_id") UUID taskId, @Column("created_at") Instant createdAt) {}
