package run.ikaros.storage.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BeginUploadRequest(
    @NotBlank @Size(max = 512) String fileName,
    @Min(0) long sizeBytes,
    @NotBlank @Size(max = 256) String mediaType,
    @NotBlank @Size(max = 128) String provider,
    @Size(max = 1024) String objectKey,
    @NotBlank @jakarta.validation.constraints.Pattern(regexp = "^[A-Fa-f0-9]{64}$") String sha256
) { }
