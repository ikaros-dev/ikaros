package run.ikaros.storage.api;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** Blob metadata exposed to attachment administrators. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdminAttachmentBlob(UUID id, String hashAlgorithm, String sha256, long sizeBytes,
                                  String mediaType, BlobAvailability availability, Instant createdAt) { }
