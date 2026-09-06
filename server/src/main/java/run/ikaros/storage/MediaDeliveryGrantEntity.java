package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_delivery_grant")
public record MediaDeliveryGrantEntity(
    @Id UUID id,
    @Column("attachment_id") UUID attachmentId,
    @Column("owner_id") UUID ownerId,
    @Column("token_hash") String tokenHash,
    String method,
    @Column("range_start") Long rangeStart,
    @Column("range_end") Long rangeEnd,
    @Column("expires_at") Instant expiresAt,
    @Column("revocation_level") DeliveryGrantRevocationLevel revocationLevel,
    @Column("revoked_at") Instant revokedAt,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {}
