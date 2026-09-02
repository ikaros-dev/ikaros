package run.ikaros.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建自定义平台角色时接受的资料。
 */
public record CreateRoleRequest(
    @NotBlank @Size(max = 96) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String code,
    @NotBlank @Size(max = 128) String name,
    @Size(max = 2000) String description
) {
}
