package run.ikaros.integration.api;

import java.time.Instant;
import java.util.UUID;

/** Stable event view exposed to consumers without leaking Outbox persistence types. */
public record DurableEvent(
    UUID id,
    String eventType,
    int schemaVersion,
    String producerSubsystem,
    String subjectType,
    UUID subjectId,
    String payloadJson,
    Instant occurredAt,
    String requestId,
    String correlationId,
    String causationId,
    UUID actorId
) { }
