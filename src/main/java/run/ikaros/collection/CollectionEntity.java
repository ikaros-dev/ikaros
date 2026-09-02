package run.ikaros.collection;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户用于组织 Resource 的逻辑集合，不承担任何物理目录语义。
 */
@Table("collection")
public record CollectionEntity(
    @Id UUID id,
    @Column("owner_id") UUID ownerId,
    @Column("parent_id") UUID parentId,
    String name,
    String description,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {
    /** Backwards-compatible constructor for root collections. */
    public CollectionEntity(UUID id, UUID ownerId, String name, String description,
                            Instant createdAt, Instant updatedAt, Long version) {
        this(id, ownerId, null, name, description, createdAt, updatedAt, version);
    }
}
