package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;

public record MetadataSyncStatusView(UUID id, UUID syncSourceId, UUID resourceId, String fieldKey,
                                     String status, UUID candidateId, Instant checkedAt) { }
