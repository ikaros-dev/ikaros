package cn.liguohao.ikaros.model.dto;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.model.entity.UserEntity;
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
        Assert.notNull(userEntity, "'userEntity' must not be null");
        BeanKit.copyProperties(userEntity, this);
    }

    public Set<RoleDTO> getRoles() {
        return roles;
    }

    public UserDTO setRoles(Set<RoleDTO> roles) {
        this.roles = roles;
        return this;
    }
}
