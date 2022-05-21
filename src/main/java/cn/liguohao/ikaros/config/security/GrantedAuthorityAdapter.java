package cn.liguohao.ikaros.config.security;

import cn.liguohao.ikaros.entity.Role;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author li-guohao
 */
public record GrantedAuthorityAdapter(Role role) implements GrantedAuthority {

    /**
     * @return 权限字符串，就是角色名称
     */
    @Override
    public String getAuthority() {
        return role.getName();
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

        return Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return role != null ? role.hashCode() : 0;
    }
}
