package run.ikaros.storage;

import java.time.Instant;

public record StorageUploadIntent(String method, String url, String objectKey, Instant expiresAt) { }
