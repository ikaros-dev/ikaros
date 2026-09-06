package run.ikaros.backup;
import java.time.Instant;
import java.util.UUID;
public record RestorePointView(UUID id, String formatVersion, String sourceInstanceId, String schemaVersion,
    String manifestDigest, RestorePointState state, VerificationLevel verificationLevel,
    VerificationStatus verificationStatus, String failureReason, long checkedObjects, long failedObjects,
    Instant createdAt, Instant publishedAt) {}
