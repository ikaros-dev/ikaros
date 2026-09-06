package run.ikaros.storage.api;

import java.util.UUID;

/**
 * Blob Placement 的 API 视图，不泄露 Provider 的敏感凭据。
 */
public record PlacementView(UUID id, String provider, StorageTier tier, String objectKey,
                            PlacementState state) {
}
