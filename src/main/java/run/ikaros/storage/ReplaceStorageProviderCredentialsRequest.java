package run.ikaros.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplaceStorageProviderCredentialsRequest(
    @NotBlank @Size(max = 256) String accessKeyId,
    @NotBlank @Size(max = 512) String secretAccessKey,
    @Size(max = 2048) String sessionToken
) { }
