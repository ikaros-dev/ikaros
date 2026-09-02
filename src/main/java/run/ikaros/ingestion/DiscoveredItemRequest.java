package run.ikaros.ingestion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record DiscoveredItemRequest(@NotBlank @Size(max = 2048) String relativeKey,
    @Min(0) long sizeBytes, Instant modifiedAt, @Size(max = 512) String etag,
    @Size(max = 256) String mediaType, @NotBlank @Size(max = 24) String availability,
    @Min(0) long scanGeneration) { }
