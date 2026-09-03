package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ingestion_candidate")
public record IngestionCandidateEntity(@Id UUID id, @Column("scan_run_id") UUID scanRunId,
    @Column("source_id") UUID sourceId, @Column("suggested_resource_type") String suggestedResourceType,
    @Column("title_hint") String titleHint, @Column("external_id_hint") String externalIdHint,
    int confidence, String fingerprint, String status, @Column("created_at") Instant createdAt,
    @Version Long version) { }
