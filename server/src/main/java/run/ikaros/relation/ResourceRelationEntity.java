package run.ikaros.relation;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 两个 Resource 之间具备类型和方向的持久化关系。
 */
@Table("resource_relation")
public record ResourceRelationEntity(
    @Id UUID id,
    @Column("source_resource_id") UUID sourceResourceId,
    @Column("target_resource_id") UUID targetResourceId,
    ResourceRelationType relationType,
    int position,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
}
