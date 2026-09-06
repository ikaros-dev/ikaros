package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_task_dependency")
public record PlanningTaskDependencyEntity(@Id UUID id, @Column("task_id") UUID taskId,
    @Column("depends_on_task_id") UUID dependsOnTaskId, PlanningTaskDependencyType type,
    @Column("created_at") Instant createdAt) {}
