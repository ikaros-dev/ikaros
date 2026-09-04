package run.ikaros.storage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BeginUploadRequest(
    @NotBlank @Size(max = 512) String fileName,
    @Min(0) long sizeBytes,
    @NotBlank @Size(max = 256) String mediaType,
    @NotBlank @Size(max = 128) String provider,
    @Size(max = 1024) String objectKey
) { }
