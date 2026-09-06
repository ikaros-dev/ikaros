package run.ikaros.authorization.api;

import java.util.List;
import java.util.UUID;

/** Authentication 签发 Token 时使用的权限快照，不暴露 Authorization 持久化模型。 */
public record PermissionSnapshot(UUID subjectId, List<String> permissionKeys) {
    public PermissionSnapshot {
        permissionKeys = List.copyOf(permissionKeys);
    }
}
