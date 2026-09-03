package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Resource 的持久化身份与生命周期；内容标题和外部身份均通过独立模型维护。
 */
@Table("resource")
public record ResourceEntity(
    @Id UUID id,
    @Column("owner_id") UUID ownerId,
    @Column("resource_type") ResourceType resourceType,
    @Column("primary_title") String primaryTitle,
    String summary,
    @Column("data_classification") ResourceClassification dataClassification,
    ResourceLifecycle lifecycle,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Column("deleted_at") Instant deletedAt,
    @Version Long version
) {
    public ResourceEntity(UUID id, UUID ownerId, ResourceType resourceType, ResourceLifecycle lifecycle,
                          Instant createdAt, Instant updatedAt, Instant deletedAt, Long version) {
        this(id, ownerId, resourceType, null, null, ResourceClassification.PRIVATE, lifecycle,
            createdAt, updatedAt, deletedAt, version);
    }
}
