package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("ingestion_source")
public record IngestionSourceEntity(
    @Id UUID id,
    @Column("owner_id") UUID ownerId,
    @Column("source_type") String sourceType,
    @Column("display_name") String displayName,
    @Column("root_reference") String rootReference,
    @Column("credential_reference") String credentialReference,
    @Column("scan_policy_json") String scanPolicyJson,
    String status,
    @Column("last_successful_scan") Instant lastSuccessfulScan,
    @Column("health_status") String healthStatus,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) { }
