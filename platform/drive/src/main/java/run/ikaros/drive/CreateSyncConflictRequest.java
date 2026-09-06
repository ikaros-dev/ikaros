package run.ikaros.drive;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateSyncConflictRequest(@NotNull UUID bindingId, @NotNull UUID nodeId,
    UUID baseRevisionId, UUID remoteRevisionId, String localFingerprint) {}
