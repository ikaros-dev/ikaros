package run.ikaros.drive;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CameraBackupScopeRequest(@NotNull CameraBackupScopeKind scopeKind, @Size(max = 512) String albumId) {}
