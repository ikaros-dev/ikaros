package run.ikaros.identity;

import java.util.List;
import java.util.UUID;

/**
 * 管理端可读取的角色与已声明权限视图。
 */
public record RoleView(UUID id, String code, String name, String description, boolean builtIn,
                       List<String> permissions) {
}
