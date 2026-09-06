package run.ikaros.resource;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("resource_creation_idempotency")
public record ResourceCreationIdempotencyEntity(
    @Id UUID id,
    @Column("owner_id") UUID ownerId,
    @Column("idempotency_key") String idempotencyKey,
    @Column("request_fingerprint") String requestFingerprint,
    @Column("resource_id") UUID resourceId,
    @Column("created_at") Instant createdAt
) {
}
