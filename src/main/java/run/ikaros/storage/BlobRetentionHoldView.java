package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record BlobRetentionHoldView(UUID id, UUID blobId, String holderType, String holderId,
    String reasonCode, Instant expiresAt, UUID createdBy, Instant createdAt, Instant releasedAt) {}
