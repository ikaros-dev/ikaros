package run.ikaros.drive;
import jakarta.validation.constraints.NotBlank;
public record CreateDriveSpaceRequest(@NotBlank String displayName) {}
