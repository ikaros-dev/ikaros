package run.ikaros.backup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CreateRestorePointRequest(@NotBlank String formatVersion, @NotBlank String sourceInstanceId,
    @NotBlank String schemaVersion, @NotBlank String manifestDigest, @NotNull VerificationLevel level) {}
