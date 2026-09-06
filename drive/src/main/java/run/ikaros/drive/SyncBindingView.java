package run.ikaros.drive;
import java.time.Instant;
import java.util.UUID;
public record SyncBindingView(UUID id, UUID userId, UUID deviceId, UUID driveSpaceId, UUID remoteRootNodeId,
    String localScopeId, String localDisplayPath, SyncSourceKind sourceKind, SyncMode mode,
    DeletePolicy deletePolicy, ConflictPolicy conflictPolicy, boolean enabled, SyncBindingState state,
    long cursor, Instant createdAt, Instant updatedAt) {}
