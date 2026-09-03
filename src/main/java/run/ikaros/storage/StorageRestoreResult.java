package run.ikaros.storage;

import java.time.Instant;

public record StorageRestoreResult(boolean readable, Instant expiresAt, String providerOperationId) {}
