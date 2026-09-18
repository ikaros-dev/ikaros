package run.ikaros.operations.api;

/** 请求或跨模块流程的审计关联信息。 */
public record AuditContext(String requestId, String correlationId) {
}
