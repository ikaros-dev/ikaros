package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;

public record DiscoveredItemView(UUID id, UUID sourceId, UUID scanRunId, String relativeKey, long sizeBytes,
    Instant modifiedAt, String etag, String mediaType, String availability, long scanGeneration) { }
