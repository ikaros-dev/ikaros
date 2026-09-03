package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record RestoreRequestContractView(UUID id, String scopeType, UUID scopeId, String status,
    int itemCount, long totalBytes, int readyItems, int failedItems, String budgetDecision, Instant createdAt) {}
