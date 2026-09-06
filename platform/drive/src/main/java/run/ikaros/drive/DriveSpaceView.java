package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record DriveSpaceView(UUID id, UUID ownerUserId, String displayName, UUID rootNodeId,
    long changeGeneration, Instant createdAt, Instant updatedAt, long version) {}
