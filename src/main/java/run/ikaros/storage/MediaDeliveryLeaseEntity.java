package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_delivery_lease")
public record MediaDeliveryLeaseEntity(
    @Id UUID id,
    @Column("attachment_id") UUID attachmentId,
    @Column("blob_id") UUID blobId,
    @Column("owner_id") UUID ownerId,
    @Column("grant_id") UUID grantId,
    @Column("lease_expires_at") Instant leaseExpiresAt,
    @Column("released_at") Instant releasedAt,
    @Column("last_heartbeat_at") Instant lastHeartbeatAt,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {}
