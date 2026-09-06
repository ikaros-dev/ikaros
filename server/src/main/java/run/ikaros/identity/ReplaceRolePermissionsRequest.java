package run.ikaros.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 角色权限的完整替换请求；权限只能来自平台注册表。 */
public record ReplaceRolePermissionsRequest(@NotNull @Valid List<PlatformPermission> permissions) { }
