package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户自定义 Resource 标签及其资源关联。
 */
@Table("resource_tag")
public record ResourceTagEntity(
    /** 标签标识。 */
    @Id UUID id,
    /** 标签所属用户。 */
    @Column("owner_id") UUID ownerId,
    /** 标签关联的 Resource。 */
    @Column("resource_id") UUID resourceId,
    /** 标签名称。 */
    String name,
    /** 标签展示颜色。 */
    String color,
    /** 标签创建时间。 */
    @Column("created_at") Instant createdAt,
    /** 标签更新时间。 */
    @Column("updated_at") Instant updatedAt,
    /** R2DBC 乐观锁版本。 */
    @Version Long version
) {
}
