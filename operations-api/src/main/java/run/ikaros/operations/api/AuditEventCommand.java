package run.ikaros.operations.api;

import java.util.UUID;

/**
 * 写入统一审计事实的公共契约。
 *
 * <p>action 使用小写点分命名，targetType 使用大写下划线命名。detailsJson 必须为不含
 * Secret 的 JSON 对象，detailsSchemaVersion 由调用方按其详情结构版本递增。</p>
 */
public record AuditEventCommand(
    AuditActorType actorType,
    UUID actorId,
    String action,
    String targetType,
    UUID targetId,
    AuditResult result,
    AuditRiskLevel riskLevel,
    String detailsJson,
    int detailsSchemaVersion,
    AuditContext context
) {
}
