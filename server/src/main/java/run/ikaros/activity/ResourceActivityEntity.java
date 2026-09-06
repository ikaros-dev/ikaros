package run.ikaros.activity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户可见的 Resource 活动记录，不承担权限审计职责。
 */
@Table("resource_activity")
public record ResourceActivityEntity(
    /** 活动记录标识。 */
    @Id UUID id,
    /** 活动所属用户。 */
    @Column("owner_id") UUID ownerId,
    /** 活动关联的 Resource。 */
    @Column("resource_id") UUID resourceId,
    /** 活动类型。 */
    @Column("activity_type") ActivityType activityType,
    /** 面向活动展示的扩展详情。 */
    String details,
    /** 活动发生时间。 */
    @Column("occurred_at") Instant occurredAt,
    /** R2DBC 乐观锁版本。 */
    @Version Long version
) {
}
