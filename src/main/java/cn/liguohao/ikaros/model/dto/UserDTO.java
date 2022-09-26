package cn.liguohao.ikaros.model.dto;

import cn.liguohao.ikaros.model.entity.UserEntity;
import java.util.Set;

/**
 * @author guohao
 * @date 2022/09/27
 */
public class UserDTO extends UserEntity {
    private Set<String> roles;

    public Set<String> getRoles() {
        return roles;
    }

    public UserDTO setRoles(Set<String> roles) {
        this.roles = roles;
        return this;
    }
}
