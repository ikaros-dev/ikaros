package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record StorageRestoreBudgetView(UUID id, long maxBytesPerRequest, int maxItemsPerRequest,
    int maxConcurrentOperations, long maxConcurrentBytes, long dailyRequestedBytes,
    long dailyProviderRestoreBytes, StorageRestoreBudgetAction overBudgetAction, Instant updatedAt) {}
