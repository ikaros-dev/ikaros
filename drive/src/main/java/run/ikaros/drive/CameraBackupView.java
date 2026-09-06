package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record CameraBackupView(UUID id, UUID bindingId, String sourceItemId, CameraBackupState state,
    UUID remoteNodeId, UUID remoteRevisionId, String contentFingerprint, String errorMessage,
    Instant updatedAt) {}
