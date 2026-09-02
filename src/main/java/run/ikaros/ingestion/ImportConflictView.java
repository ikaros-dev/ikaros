package run.ikaros.ingestion;
import java.time.Instant;
import java.util.UUID;
public record ImportConflictView(UUID id, UUID planId, UUID candidateId, String reason, int confidence,
    ImportConflictStatus status, String resolution, Long version, Instant createdAt, Instant resolvedAt) { }
