package run.ikaros.server.model.dto;

import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.entity.UserEntity;
import java.util.HashSet;
import java.util.Set;

/**
 * @author guohao
 * @date 2022/09/27
 */
public class UserDTO extends UserEntity {
    private Set<RoleDTO> roles = new HashSet<>();

    public UserDTO() {
    }

    public UserDTO(UserEntity userEntity) {
        AssertUtils.notNull(userEntity, "'userEntity' must not be null");
        BeanUtils.copyProperties(userEntity, this);
    }

    public Set<RoleDTO> getRoles() {
        return roles;
    }

    public UserDTO setRoles(Set<RoleDTO> roles) {
        this.roles = roles;
        return this;
    }
}
