package cn.liguohao.ikaros.config.security;

import cn.liguohao.ikaros.persistence.structural.entity.RoleEntity;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author li-guohao
 */
public record GrantedAuthorityAdapter(RoleEntity roleEntity) implements GrantedAuthority {

    /**
     * @return 权限字符串，就是角色名称
     */
    @Override
    public String getAuthority() {
        return roleEntity.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GrantedAuthorityAdapter that = (GrantedAuthorityAdapter) o;

        return Objects.equals(roleEntity, that.roleEntity);
    }

    @Override
    public int hashCode() {
        return roleEntity != null ? roleEntity.hashCode() : 0;
    }
}
