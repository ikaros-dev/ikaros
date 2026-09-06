package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record SyncMappingView(UUID id, UUID bindingId, String localItemId, UUID remoteNodeId,
    UUID lastSyncedRevisionId, String lastSyncedFingerprint, long lastSeenRemoteVersion,
    SyncMappingState state, Instant updatedAt) {}
