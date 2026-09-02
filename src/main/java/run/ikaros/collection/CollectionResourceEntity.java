package run.ikaros.collection;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Collection 与 Resource 的有序成员关系。
 */
@Table("collection_resource")
public record CollectionResourceEntity(
    @Id UUID id,
    @Column("collection_id") UUID collectionId,
    @Column("resource_id") UUID resourceId,
    int position,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
}
