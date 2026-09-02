package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户对 Resource 的收藏关系。
 */
@Table("resource_favorite")
public record FavoriteEntity(
    /** 收藏关系标识。 */
    @Id UUID id,
    /** 收藏关系所属用户。 */
    @Column("owner_id") UUID ownerId,
    /** 被收藏的 Resource。 */
    @Column("resource_id") UUID resourceId,
    /** 收藏创建时间。 */
    @Column("created_at") Instant createdAt,
    /** R2DBC 乐观锁版本。 */
    @Version Long version
) {
}
