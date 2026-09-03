package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户登录会话及短期 Step-up 验证状态；不保存会话或刷新令牌原文。
 */
@Table("security_session")
public record SecuritySessionEntity(
    @Id UUID id,
    @Column("user_id") UUID userId,
    @Column("security_version") long securityVersion,
    @Column("login_method") String loginMethod,
    @Column("current_svl") int currentSvl,
    @Column("verified_at") Instant verifiedAt,
    @Column("verification_expires_at") Instant verificationExpiresAt,
    @Column("expires_at") Instant expiresAt,
    @Column("revoked_at") Instant revokedAt,
    @Column("last_active_at") Instant lastActiveAt,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
    public SecuritySessionEntity(UUID id, UUID userId, String loginMethod, int currentSvl,
                                 Instant verifiedAt, Instant verificationExpiresAt, Instant expiresAt,
                                 Instant revokedAt, Instant lastActiveAt, Instant createdAt, Long version) {
        this(id, userId, 0L, loginMethod, currentSvl, verifiedAt, verificationExpiresAt, expiresAt,
            revokedAt, lastActiveAt, createdAt, version);
    }
}
