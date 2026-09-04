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
    @Column("durability_role") PlacementDurabilityRole durabilityRole,
    @Column("evictable") boolean evictable,
    @Column("gc_protected") boolean gcProtected,
    @Column("retention_until") Instant retentionUntil,
    @Column("minimum_retention_until") Instant minimumRetentionUntil,
    @Column("last_accessed_at") Instant lastAccessedAt,
    @Column("source_placement_id") UUID sourcePlacementId,
    @Column("verified_at") Instant verifiedAt,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
    /** Compatibility constructor for existing upload and test call sites. */
    public BlobPlacementEntity(UUID id, UUID blobId, String provider, StorageTier storageTier,
        String objectKey, PlacementState placementState, Instant verifiedAt, Instant createdAt, Long version) {
        this(id, blobId, provider, storageTier, objectKey, placementState, PlacementDurabilityRole.PRIMARY,
            false, false, null, null, null, null, verifiedAt, createdAt, version);
    }
}
