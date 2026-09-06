package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("planning_tag")
public record PlanningTagEntity(@Id UUID id, @Column("owner_id") UUID ownerId, String name,
    String color, @Column("created_at") Instant createdAt) {}
