package run.ikaros.ingestion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IngestionSourceView(UUID id, IngestionSourceType type, String displayName, String rootReference,
                                  boolean credentialConfigured, Map<String, Object> scanPolicy,
                                  IngestionSourceStatus status, Instant lastSuccessfulScan,
                                  String healthStatus, Instant createdAt, Instant updatedAt) { }
