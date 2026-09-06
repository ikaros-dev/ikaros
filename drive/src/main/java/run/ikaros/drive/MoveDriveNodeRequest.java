package run.ikaros.drive;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record MoveDriveNodeRequest(@NotNull UUID parentId, long expectedVersion) {}
