package run.ikaros.storage;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import run.ikaros.storage.api.StorageTier;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StorageProviderCreateRequest(
    @NotBlank @Size(max = 256) String providerKey,
    @NotBlank @Size(max = 128) String providerType,
    @NotBlank @Size(max = 256) String displayName,
    @NotNull StorageTier tier,
    @NotNull Map<String, Object> capabilities,
    @Size(max = 512) String credentialRef,
    Map<String, Object> configuration
) { }
