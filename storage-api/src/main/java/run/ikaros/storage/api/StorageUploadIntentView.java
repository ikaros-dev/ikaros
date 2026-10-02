package run.ikaros.storage.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StorageUploadIntentView(String provider, StorageTier tier, String method, String url,
                                      String objectKey, Instant expiresAt, String sha256, boolean deduplicated,
                                      UUID sessionId, Map<String, String> requiredHeaders) {
    public StorageUploadIntentView(String provider, StorageTier tier, String method, String url,
                                   String objectKey, Instant expiresAt, String sha256, boolean deduplicated,
                                   UUID sessionId) {
        this(provider, tier, method, url, objectKey, expiresAt, sha256, deduplicated, sessionId, Map.of());
    }

    public StorageUploadIntentView(String provider, StorageTier tier, String method, String url,
                                   String objectKey, Instant expiresAt, String sha256, boolean deduplicated) {
        this(provider, tier, method, url, objectKey, expiresAt, sha256, deduplicated, null, Map.of());
    }
}
