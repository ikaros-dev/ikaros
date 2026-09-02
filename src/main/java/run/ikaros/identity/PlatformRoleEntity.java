package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 平台角色的持久化模型，角色与权限通过独立绑定表关联。
 */
@Table("platform_role")
public record PlatformRoleEntity(
    @Id UUID id,
    @Column("role_code") String code,
    String name,
    String description,
    @Column("built_in") boolean builtIn,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {
}
