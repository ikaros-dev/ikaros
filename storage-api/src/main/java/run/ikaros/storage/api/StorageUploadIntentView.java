package run.ikaros.storage.api;

import java.time.Instant;
import java.util.UUID;

public record StorageUploadIntentView(String provider, StorageTier tier, String method, String url,
                                      String objectKey, Instant expiresAt, String sha256, boolean deduplicated,
                                      UUID sessionId) {
    public StorageUploadIntentView(String provider, StorageTier tier, String method, String url,
                                   String objectKey, Instant expiresAt, String sha256, boolean deduplicated) {
        this(provider, tier, method, url, objectKey, expiresAt, sha256, deduplicated, null);
    }
}
