package run.ikaros.progress;

import java.time.Instant;
import java.util.UUID;

/**
 * Resource 消费进度 API 视图。
 */
public record ResourceProgressView(
    /** 进度记录标识。 */
    UUID id,
    /** Resource 标识。 */
    UUID resourceId,
    /** 进度单位类型。 */
    ProgressType type,
    /** 当前进度值。 */
    long position,
    /** 总进度值。 */
    Long total,
    /** 是否已完成。 */
    boolean completed,
    /** 最近更新时间。 */
    Instant updatedAt,
    Long version
) {
    public ResourceProgressView(UUID id, UUID resourceId, ProgressType type, long position, Long total,
                                boolean completed, Instant updatedAt) {
        this(id, resourceId, type, position, total, completed, updatedAt, null);
    }
}
