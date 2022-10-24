package run.ikaros.server.model.dto;

import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.entity.RoleEntity;
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
        AssertUtils.notNull(roleEntity, "'roleEntity' must not be null");
        BeanUtils.copyProperties(roleEntity, this);
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public RoleDTO setPermissions(List<String> permissions) {
        this.permissions = permissions;
        return this;
    }
}
