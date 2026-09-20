package run.ikaros.operations.api;

/** 审计操作的确定结果；UNKNOWN 仅用于迁移前的历史记录。 */
public enum AuditResult {
    SUCCESS,
    FAILURE,
    DENIED,
    UNKNOWN
}
