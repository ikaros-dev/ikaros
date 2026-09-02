package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_task_tag")
public record PlanningTaskTagEntity(@Id UUID id, @Column("task_id") UUID taskId,
    @Column("tag_id") UUID tagId, @Column("created_at") Instant createdAt) {}
