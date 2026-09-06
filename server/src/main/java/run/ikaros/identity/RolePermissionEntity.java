package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 角色与已声明平台权限的绑定记录。
 */
@Table("role_permission")
public record RolePermissionEntity(
    @Id UUID id,
    @Column("role_id") UUID roleId,
    @Column("permission_key") String permissionKey,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
}
