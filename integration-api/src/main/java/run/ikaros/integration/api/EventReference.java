package run.ikaros.integration.api;

import java.util.UUID;

/** 已写入 Durable Event 的公开引用。 */
public record EventReference(UUID eventId, String eventType, int schemaVersion) { }
