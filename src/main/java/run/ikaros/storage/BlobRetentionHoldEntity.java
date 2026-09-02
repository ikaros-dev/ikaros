package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("blob_retention_hold")
public record BlobRetentionHoldEntity(@Id UUID id, @Column("blob_id") UUID blobId,
    @Column("holder_type") String holderType, @Column("holder_id") String holderId,
    @Column("reason_code") String reasonCode, @Column("expires_at") Instant expiresAt,
    @Column("created_by") UUID createdBy, @Column("created_at") Instant createdAt,
    @Column("released_at") Instant releasedAt, @Version Long version) {}
