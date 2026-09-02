package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record DriveNodeView(UUID id, UUID driveSpaceId, UUID parentId, DriveNodeType nodeType, String name,
    String normalizedName, DriveLifecycle lifecycle, UUID currentRevisionId, long nodeVersion,
    Instant createdAt, Instant updatedAt) {}
