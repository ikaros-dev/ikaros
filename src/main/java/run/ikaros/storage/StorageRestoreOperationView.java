package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record StorageRestoreOperationView(UUID id, UUID placementId, String providerRestoreClass,
    long restoreGeneration, StorageRestoreOperationStatus status, String providerOperationId,
    Instant restoreExpiresAt, String errorSummary, Instant createdAt, Instant updatedAt) {}
