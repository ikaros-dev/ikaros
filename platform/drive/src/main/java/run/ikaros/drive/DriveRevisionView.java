package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record DriveRevisionView(UUID id, UUID fileNodeId, long revisionNo, UUID attachmentId,
    String contentFingerprint, Instant contentModifiedAt, Instant createdAt, UUID createdBy) {}
