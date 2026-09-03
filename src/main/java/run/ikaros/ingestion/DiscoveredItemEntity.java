package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ingestion_discovered_item")
public record DiscoveredItemEntity(@Id UUID id, @Column("source_id") UUID sourceId,
    @Column("scan_run_id") UUID scanRunId, @Column("relative_key") String relativeKey,
    @Column("size_bytes") long sizeBytes, @Column("modified_at") Instant modifiedAt,
    String etag, @Column("media_type") String mediaType, String availability,
    @Column("scan_generation") long scanGeneration, @Column("created_at") Instant createdAt,
    @Version Long version) { }
