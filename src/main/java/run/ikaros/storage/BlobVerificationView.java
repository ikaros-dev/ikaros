package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

public record BlobVerificationView(UUID blobId, UUID placementId, BlobIntegrityStatus status,
    String actualSha256, long actualSize, Instant verifiedAt) {}
