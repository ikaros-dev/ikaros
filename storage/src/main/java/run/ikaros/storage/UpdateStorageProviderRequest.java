package run.ikaros.storage;

import run.ikaros.storage.api.*;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** Storage Provider 的部分更新请求；Provider key 与启停状态由专用命令维护。 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateStorageProviderRequest(
    StorageProviderType providerType,
    @Size(max = 256) String displayName,
    StorageTier tier,
    @Size(max = 512) String secretReference,
    Map<String, Object> configuration
) {
}
