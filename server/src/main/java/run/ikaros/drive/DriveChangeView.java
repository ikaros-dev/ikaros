package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record DriveChangeView(UUID id, UUID driveSpaceId, long sequence, UUID nodeId,
    DriveMutationKind mutationKind, long nodeVersion, UUID revisionId, Instant occurredAt) {}
