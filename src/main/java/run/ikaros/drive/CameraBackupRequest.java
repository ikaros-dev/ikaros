package run.ikaros.drive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CameraBackupRequest(@NotBlank String sourceItemId, @NotNull CameraBackupState state,
    UUID remoteNodeId, UUID remoteRevisionId, String contentFingerprint, String errorMessage) {}
