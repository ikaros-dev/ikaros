package run.ikaros.progress;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户在一个 Resource 上按进度类型保存的消费进度。
 */
@Table("resource_progress")
public record ResourceProgressEntity(
    /** 进度记录标识。 */
    @Id UUID id,
    /** 进度所属用户。 */
    @Column("owner_id") UUID ownerId,
    /** 进度关联的 Resource。 */
    @Column("resource_id") UUID resourceId,
    /** 进度单位类型。 */
    @Column("progress_type") ProgressType progressType,
    /** 当前进度值，单位由 progressType 决定。 */
    @Column("position_value") long positionValue,
    /** 总进度值，可为空。 */
    @Column("total_value") Long totalValue,
    /** 是否已完成。 */
    boolean completed,
    /** 最近更新时间。 */
    @Column("updated_at") Instant updatedAt,
    /** R2DBC 乐观锁版本。 */
    @Version Long version
) {
}
