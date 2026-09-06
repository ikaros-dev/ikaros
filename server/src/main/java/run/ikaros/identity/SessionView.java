package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * 安全会话的脱敏管理视图，不暴露访问令牌或刷新令牌。
 */
public record SessionView(UUID id, UUID userId, String loginMethod, SecurityVerificationLevel currentSvl,
                          Instant verifiedAt, Instant verificationExpiresAt, Instant expiresAt,
                          Instant lastActiveAt) {
}
