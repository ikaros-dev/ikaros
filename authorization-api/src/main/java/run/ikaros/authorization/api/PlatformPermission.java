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
    INGESTION_SOURCE_MANAGE("ingestion.source.manage", "管理导入来源"),
    ACCOUNT_SELF_READ("account.self.read", "查看个人资料"),
    ACCOUNT_PREFERENCE_READ("account.preference.read", "查看个人偏好"),
    ACCOUNT_NOTIFICATION_READ("account.notification.read", "查看个人通知设置"),
    ACCOUNT_SECURITY_READ("account.security.read", "查看账户安全"),
    USER_READ("user.read", "查看用户管理"),
    ROLE_READ("role.read", "查看角色与权限"),
    SECURITY_AUTHENTICATION_READ("security.authentication.read", "查看身份认证配置"),
    INTEGRATION_READ("integration.read", "查看平台集成"),
    NOTIFICATION_READ("notification.read", "查看通知中心"),
    AUDIT_READ("audit.read", "查看审计日志"),
    PLATFORM_READ("platform.read", "查看系统参数"),
    SYSTEM_HEALTH_READ("system.health.read", "查看系统健康"),
    SYSTEM_DIAGNOSTICS_READ("system.diagnostics.read", "查看系统诊断"),
    COLLECTION_READ("collection.read", "查看资源集合"),
    SEARCH_USE("search.use", "使用资源搜索"),
    STORAGE_POLICY_READ("storage.policy.read", "查看存储策略"),
    STORAGE_ARCHIVE_READ("storage.archive.read", "查看归档管理"),
    STORAGE_MAINTENANCE_READ("storage.maintenance.read", "查看存储维护"),
    BACKUP_READ("backup.read", "查看备份管理"),
    DRIVE_SPACE_READ("drive.space.read", "查看云盘空间"),
    DRIVE_FILE_READ("drive.file.read", "查看云盘文件"),
    DRIVE_TRANSFER_READ("drive.transfer.read", "查看云盘传输"),
    DRIVE_SYNC_READ("drive.sync.read", "查看云盘同步"),
    DRIVE_CONFLICT_READ("drive.conflict.read", "查看云盘冲突"),
    DRIVE_TRASH_READ("drive.trash.read", "查看云盘回收站"),
    DRIVE_SETTINGS_READ("drive.settings.read", "查看云盘设置"),
    DOCUMENT_READ("document.read", "查看文档"),
    MEDIA_READ("media.read", "查看媒体"),
    PLANNING_READ("planning.read", "查看计划"),
    FINANCE_READ("finance.read", "查看财务"),
    PRIVATE_NOTE_READ("private_note.read", "查看私密笔记"),
    PASSWORD_READ("password.read", "查看密码库"),
    AI_READ("ai.read", "查看 AI 应用"),
    SHARE_READ("share.read", "查看分享"),
    ANALYTICS_READ("analytics.read", "查看数据分析"),
    AUTOMATION_READ("automation.read", "查看自动化"),
    DASHBOARD_READ("dashboard.read", "查看 Overview"),
    INGESTION_READ("ingestion.read", "查看 Add Content"),
    ACTIVITY_READ("activity.read", "查看 Activity"),
    STORAGE_READ("storage.read", "查看 Storage"),
    APP_READ("app.read", "查看 Apps"),
    SYSTEM_READ("system.read", "查看 System");

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
