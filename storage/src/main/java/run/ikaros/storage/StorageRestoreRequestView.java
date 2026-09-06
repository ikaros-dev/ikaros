package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record StorageRestoreRequestView(UUID id, UUID actorId, StorageRestoreScope scope, UUID scopeId,
    StorageRestoreRequestStatus status, int totalItems, int completedItems, long totalBytes,
    String errorSummary, UUID backgroundTaskId, Instant createdAt, Instant updatedAt,
    String budgetDecision) {}
