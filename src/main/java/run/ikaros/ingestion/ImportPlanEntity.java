package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ingestion_import_plan")
public record ImportPlanEntity(@Id UUID id, @Column("scan_run_id") UUID scanRunId,
    @Column("owner_id") UUID ownerId, boolean dryRun, String status,
    @Column("policy_snapshot_json") String policySnapshotJson, @Column("generated_at") Instant generatedAt,
    @Version Long version) { }
