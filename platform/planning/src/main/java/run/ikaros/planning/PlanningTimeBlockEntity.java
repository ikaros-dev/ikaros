package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_time_block")
public record PlanningTimeBlockEntity(@Id UUID id, @Column("owner_id") UUID ownerId, String title,
    @Column("task_id") UUID taskId, @Column("start_at") Instant startAt, @Column("end_at") Instant endAt,
    PlanningTimeBlockKind kind, PlanningTimeBlockStatus status, @Column("time_zone") String timeZone,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt, @Version Long version) {}
