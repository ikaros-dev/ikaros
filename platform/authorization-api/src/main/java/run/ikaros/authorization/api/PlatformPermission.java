package run.ikaros.authorization.api;

import java.util.Arrays;
import java.util.List;

/** 由平台核心显式声明的管理能力；角色只能引用这些能力。 */
public enum PlatformPermission {
    SYSTEM_USER_READ("system.user.read", "查看用户"),
    SYSTEM_USER_MANAGE("system.user.manage", "管理用户"),
    SYSTEM_ROLE_READ("system.role.read", "查看角色"),
    SYSTEM_ROLE_MANAGE("system.role.manage", "管理角色"),
    SYSTEM_SESSION_READ("system.session.read", "查看会话"),
    SYSTEM_SESSION_MANAGE("system.session.manage", "管理会话"),
    SYSTEM_AUDIT_READ("system.audit.read", "查看审计记录"),
    RESOURCE_READ("resource.read", "读取资源"),
    RESOURCE_WRITE("resource.write", "编辑资源"),
    RESOURCE_DELETE("resource.delete", "删除资源"),
    RESOURCE_DOWNLOAD("resource.download", "下载资源"),
    RESOURCE_SHARE("resource.share", "分享资源"),
    STORAGE_PROVIDER_READ("storage.provider.read", "查看存储 Provider"),
    STORAGE_PROVIDER_MANAGE("storage.provider.manage", "管理存储 Provider"),
    STORAGE_DELIVERY_READ("storage.delivery.read", "查看 Delivery Provider 与 Binding"),
    STORAGE_DELIVERY_MANAGE("storage.delivery.manage", "管理 Delivery Provider 与 Binding"),
    STORAGE_TIERING_MANAGE("storage.tiering.manage", "管理存储分层与恢复预算"),
    STORAGE_RESTORE_REQUEST("storage.restore.request", "请求媒体恢复"),
    STORAGE_RESTORE_READ("storage.restore.read", "查看媒体恢复状态"),
    STORAGE_RESTORE_MANAGE("storage.restore.manage", "管理媒体恢复任务"),
    INGESTION_SOURCE_MANAGE("ingestion.source.manage", "管理导入来源");

    private final String key;
    private final String displayName;

    PlatformPermission(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public static PlatformPermission fromKey(String key) {
        return Arrays.stream(values())
            .filter(permission -> permission.key.equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未声明的平台权限: " + key));
    }

    public static List<String> registeredKeys() {
        return Arrays.stream(values()).map(PlatformPermission::key).toList();
    }
}
