package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record DriveTombstoneView(UUID id, UUID spaceId, UUID nodeId, long sequence, long nodeVersion,
    TombstoneLifecycle lifecycle, UUID previousParentId, String previousName, Instant deletedAt,
    Instant retentionDeadline) {}
