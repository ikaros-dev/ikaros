package run.ikaros.storage;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record BlobRetentionHoldRequest(@NotBlank String holderType, @NotBlank String holderId,
    @NotBlank String reasonCode, Instant expiresAt) {}
