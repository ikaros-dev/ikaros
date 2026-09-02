package run.ikaros.storage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StorageRestoreBudgetRequest(@Min(1) long maxBytesPerRequest, @Min(1) int maxItemsPerRequest,
    @Min(1) int maxConcurrentOperations, @Min(1) long maxConcurrentBytes, @Min(1) long dailyRequestedBytes,
    @Min(1) long dailyProviderRestoreBytes, @NotNull StorageRestoreBudgetAction overBudgetAction) {}
