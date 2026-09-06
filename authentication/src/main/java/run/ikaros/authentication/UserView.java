package run.ikaros.authentication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 管理端可读取的平台用户视图，不暴露凭据或认证令牌。
 */
public record UserView(UUID id, String username, String displayName, String email, UserStatus status,
                       List<String> roleCodes, Instant createdAt, Instant lastLoginAt) {
}
