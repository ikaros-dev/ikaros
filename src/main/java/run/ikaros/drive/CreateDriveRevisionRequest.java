package run.ikaros.drive;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateDriveRevisionRequest(@NotNull UUID attachmentId, @NotNull Long expectedNodeVersion,
    String contentFingerprint, String operationId) {}
