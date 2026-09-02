package run.ikaros.planning;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("planning_task")
public record PlanningTaskEntity(@Id UUID id, @Column("owner_id") UUID ownerId, String title,
    String description, PlanningTaskStatus status, PlanningTaskPriority priority, Instant deadline,
    @Column("project_id") UUID projectId, @Column("parent_task_id") UUID parentTaskId,
    @Column("completed_at") Instant completedAt, @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt, @Version Long version) {}
