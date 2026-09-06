package run.ikaros.integration.api;

import java.util.UUID;

/** 业务模块提交给 Integration 的事件事实，不暴露 Outbox Persistence 类型。 */
public record EventAppendRequest(
    String eventType,
    int schemaVersion,
    String producerSubsystem,
    String subjectType,
    UUID subjectId,
    String payloadJson
) { }
