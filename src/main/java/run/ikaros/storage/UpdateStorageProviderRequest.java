package run.ikaros.storage;

import jakarta.validation.constraints.Size;
import java.util.Map;

/** Storage Provider 的部分更新请求；Provider key 与状态由专用命令维护。 */
public record UpdateStorageProviderRequest(
    @Size(max = 128) String providerType,
    StorageTier tier,
    @Size(max = 512) String secretReference,
    Map<String, Object> metadata
) {
}
