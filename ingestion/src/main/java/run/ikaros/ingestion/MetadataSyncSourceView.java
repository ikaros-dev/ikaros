package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;

public record MetadataSyncSourceView(
    UUID id,
    String providerKey,
    String displayName,
    boolean credentialConfigured,
    String refreshSchedule,
    MetadataSyncSourceStatus status,
    Instant createdAt,
    Instant updatedAt,
    Long version
) { }
