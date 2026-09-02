package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Blob 在一个 Storage Provider 中的实际持久化位置或副本。
 */
@Table("blob_placement")
public record BlobPlacementEntity(
    @Id UUID id,
    @Column("blob_id") UUID blobId,
    String provider,
    @Column("storage_tier") StorageTier storageTier,
    @Column("object_key") String objectKey,
    @Column("placement_state") PlacementState placementState,
    @Column("verified_at") Instant verifiedAt,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
}
