package run.ikaros.drive;
import jakarta.validation.constraints.NotBlank;
public record RenameDriveNodeRequest(@NotBlank String name, long expectedVersion) {}
