package run.ikaros.authorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 更新自定义平台角色的可变资料；角色编码一经创建不可变。 */
public record UpdateRoleRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 2000) String description
) {
}
