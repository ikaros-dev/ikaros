package run.ikaros.drive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record UpsertSyncMappingRequest(@NotBlank String localItemId, @NotNull UUID remoteNodeId,
    UUID lastSyncedRevisionId, String lastSyncedFingerprint, long lastSeenRemoteVersion,
    SyncMappingState state) {}
