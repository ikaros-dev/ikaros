package run.ikaros.drive;

import java.time.Instant;
import java.util.UUID;

public record CameraBackupScopeView(UUID bindingId, CameraBackupScopeKind scopeKind, String albumId,
    String localScopeId, String localDisplayPath, Instant updatedAt) {}
