package run.ikaros.drive;
import jakarta.validation.constraints.NotBlank;
public record RegisterDeviceRequest(@NotBlank String installationId, @NotBlank String displayName,
    @NotBlank String platform, String appVersion) {}
