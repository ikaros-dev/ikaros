package run.ikaros.storage.api;

import java.time.Instant;
import java.util.UUID;

/** Stable storage restore request result shared with domain applications. */
public record StorageRestoreSubmissionView(UUID id, String scopeType, UUID scopeId, String status,
    int totalItems, long totalBytes, int completedItems, int failedItems, String budgetDecision,
    Instant createdAt) {
}
