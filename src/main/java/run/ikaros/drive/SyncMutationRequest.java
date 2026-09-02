package run.ikaros.drive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record SyncMutationRequest(@NotBlank String operationId, @NotNull UUID nodeId,
    @NotNull SyncMutationKind kind, long expectedVersion, String name, UUID parentId) {}
