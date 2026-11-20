package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** User access role for a Resource. */
@Table("user_resource")
public record UserResourceEntity(
    @Id UUID id,
    @Column("user_id") UUID userId,
    @Column("resource_id") UUID resourceId,
    String role,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {}
