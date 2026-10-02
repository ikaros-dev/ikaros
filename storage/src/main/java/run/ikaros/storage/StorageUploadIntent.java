package run.ikaros.storage;

import java.time.Instant;
import java.util.Map;

public record StorageUploadIntent(String method, String url, String objectKey, Instant expiresAt,
                                  Map<String, String> requiredHeaders) {
    public StorageUploadIntent(String method, String url, String objectKey, Instant expiresAt) {
        this(method, url, objectKey, expiresAt, Map.of());
    }
}
