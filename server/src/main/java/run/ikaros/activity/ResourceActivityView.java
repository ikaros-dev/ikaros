package run.ikaros.activity;

import java.time.Instant;
import java.util.UUID;

/**
 * Resource Activity API 视图。
 */
public record ResourceActivityView(
    /** 活动记录标识。 */
    UUID id,
    /** 关联的 Resource 标识。 */
    UUID resourceId,
    /** 活动类型。 */
    ActivityType type,
    /** 活动详情。 */
    String details,
    /** 活动发生时间。 */
    Instant occurredAt
) {
}
