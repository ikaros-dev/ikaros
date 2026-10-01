package run.ikaros.storage;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ReplaceStorageProviderCredentialsRequest(
    @NotBlank @Size(max = 256) String accessKeyId,
    @NotBlank @Size(max = 512) String secretAccessKey,
    @Size(max = 2048) String sessionToken
) { }
