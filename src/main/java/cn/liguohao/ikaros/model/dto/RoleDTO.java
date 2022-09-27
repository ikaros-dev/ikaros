package cn.liguohao.ikaros.model.dto;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.model.entity.RoleEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * @author guohao
 * @date 2022/09/28
 */
public class RoleDTO extends RoleEntity {

    private List<String> permissions = new ArrayList<>();

    public RoleDTO() {
    }

    public RoleDTO(RoleEntity roleEntity) {
        Assert.notNull(roleEntity, "'roleEntity' must not be null");
        BeanKit.copyProperties(roleEntity, this);
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public RoleDTO setPermissions(List<String> permissions) {
        this.permissions = permissions;
        return this;
    }
}
