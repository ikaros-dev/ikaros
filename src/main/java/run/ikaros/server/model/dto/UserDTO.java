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
    private Set<String> roles = new HashSet<>();

    public UserDTO() {
    }

    public UserDTO(UserEntity userEntity) {
        AssertUtils.notNull(userEntity, "'userEntity' must not be null");
        BeanUtils.copyProperties(userEntity, this, Set.of("password"));
    }

    public Set<String> getRoles() {
        return roles;
    }

    public UserDTO setRoles(Set<String> roles) {
        this.roles = roles;
        return this;
    }
}
