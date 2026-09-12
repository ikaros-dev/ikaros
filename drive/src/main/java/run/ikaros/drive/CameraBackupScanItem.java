package run.ikaros.drive;

import jakarta.validation.constraints.NotBlank;

/** Stable identity and content fingerprint reported by a camera-roll scanner. */
public record CameraBackupScanItem(@NotBlank String sourceItemId, @NotBlank String contentFingerprint) {}
