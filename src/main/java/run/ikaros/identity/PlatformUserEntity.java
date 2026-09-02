package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 平台用户身份的持久化模型，不包含任何凭据或密码明文。
 */
@Table("platform_user")
public record PlatformUserEntity(
    @Id UUID id,
    String username,
    @Column("display_name") String displayName,
    String email,
    UserStatus status,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Column("last_login_at") Instant lastLoginAt,
    @Version Long version
) {
}
