package run.ikaros.storage;

import java.time.Instant;

public record StorageUploadIntentView(String provider, StorageTier tier, String method, String url,
                                      String objectKey, Instant expiresAt, String sha256, boolean deduplicated) { }
