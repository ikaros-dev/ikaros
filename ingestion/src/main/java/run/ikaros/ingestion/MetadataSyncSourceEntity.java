package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("metadata_sync_source")
public record MetadataSyncSourceEntity(
    @Id UUID id,
    @Column("owner_id") UUID ownerId,
    @Column("provider_key") String providerKey,
    @Column("display_name") String displayName,
    @Column("credential_reference") String credentialReference,
    @Column("refresh_schedule") String refreshSchedule,
    String status,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) { }
