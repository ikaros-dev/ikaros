package run.ikaros.operations.api;

/** 审计操作的风险等级；UNKNOWN 仅用于迁移前的历史记录。 */
public enum AuditRiskLevel {
    NORMAL,
    SENSITIVE,
    HIGH,
    UNKNOWN
}
