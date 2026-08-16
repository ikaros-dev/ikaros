package run.ikaros.api.core.binding;

/** 扫描预览中媒体文件的业务角色. */
public enum MediaRole {
    /** 已确认的主资源. */
    PRIMARY,
    /** 已唯一自动关联的附属资源. */
    AUTO_ASSOCIATED,
    /** 需要用户确认关联的资源. */
    PENDING_CONFIRMATION,
    /** 未关联的可识别资源. */
    UNASSOCIATED,
    /** 未识别的普通文件. */
    UNKNOWN
}
