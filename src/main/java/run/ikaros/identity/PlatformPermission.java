package run.ikaros.identity;

import java.util.Arrays;
import java.util.List;

/**
 * 由平台核心显式声明的管理能力；角色只能引用这些能力，不能任意创造权限字符串。
 */
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
    STORAGE_PROVIDER_MANAGE("storage.provider.manage", "管理存储 Provider"),
    INGESTION_SOURCE_MANAGE("ingestion.source.manage", "管理导入来源");

    /** 权限的稳定标识。 */
    private final String key;
    /** 管理端显示名称。 */
    private final String displayName;

    PlatformPermission(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    /**
     * 获取稳定权限标识。
     *
     * @return 权限键
     */
    public String key() {
        return key;
    }

    /**
     * 获取面向管理员的权限名称。
     *
     * @return 中文权限名称
     */
    public String displayName() {
        return displayName;
    }

    /**
     * 根据稳定标识查询已声明的权限。
     *
     * @param key 权限键
     * @return 平台权限
     */
    public static PlatformPermission fromKey(String key) {
        return Arrays.stream(values())
            .filter(permission -> permission.key.equals(key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未声明的平台权限: " + key));
    }

    /** 返回权限注册表快照，避免调用方自行拼接权限键。 */
    public static List<String> registeredKeys() {
        return Arrays.stream(values()).map(PlatformPermission::key).toList();
    }
}
