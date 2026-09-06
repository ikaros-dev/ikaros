package run.ikaros.activity;

import jakarta.validation.constraints.Size;

/**
 * 记录 Resource Activity 的请求。
 *
 * @param type 活动类型
 * @param details 可选的非敏感扩展详情
 */
public record RecordActivityRequest(
    ActivityType type,
    @Size(max = 2000) String details
) {
}
