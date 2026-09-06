package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户与平台角色的多对多绑定记录。
 */
@Table("user_role")
public record UserRoleEntity(
    @Id UUID id,
    @Column("user_id") UUID userId,
    @Column("role_id") UUID roleId,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
}
