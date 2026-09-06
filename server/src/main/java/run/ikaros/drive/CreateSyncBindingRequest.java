package run.ikaros.drive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateSyncBindingRequest(@NotNull UUID deviceId, @NotNull UUID driveSpaceId,
    @NotNull UUID remoteRootNodeId, @NotBlank String localScopeId, String localDisplayPath,
    @NotNull SyncSourceKind sourceKind, @NotNull SyncMode mode, DeletePolicy deletePolicy,
    ConflictPolicy conflictPolicy) {}
