package run.ikaros.drive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateDriveNodeRequest(@NotNull DriveNodeType nodeType, @NotBlank String name, UUID parentId) {}
