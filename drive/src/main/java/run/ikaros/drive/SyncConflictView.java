package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record SyncConflictView(UUID id, UUID bindingId, UUID nodeId, UUID baseRevisionId,
    UUID remoteRevisionId, String localFingerprint, SyncConflictState state, Instant detectedAt,
    Instant resolvedAt, UUID resolvedBy) {}
