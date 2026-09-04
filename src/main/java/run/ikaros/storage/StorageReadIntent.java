package run.ikaros.storage;

import java.time.Instant;

public record StorageReadIntent(String method, String url, Instant expiresAt) { }
