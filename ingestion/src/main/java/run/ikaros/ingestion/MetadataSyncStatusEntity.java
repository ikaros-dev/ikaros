package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("metadata_sync_status")
public record MetadataSyncStatusEntity(
    @Id UUID id,
    @Column("owner_id") UUID ownerId,
    @Column("sync_source_id") UUID syncSourceId,
    @Column("resource_id") UUID resourceId,
    @Column("field_key") String fieldKey,
    String status,
    @Column("candidate_id") UUID candidateId,
    @Column("checked_at") Instant checkedAt
) { }
