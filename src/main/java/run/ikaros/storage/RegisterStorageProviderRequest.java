package run.ikaros.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record RegisterStorageProviderRequest(
    @NotBlank @Size(max = 256) String providerKey,
    @NotBlank @Size(max = 128) String providerType,
    @NotNull StorageTier tier,
    @NotBlank @Size(max = 512) String secretReference,
    Map<String, Object> metadata
) { }
