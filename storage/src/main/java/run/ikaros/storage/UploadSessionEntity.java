package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.storage.api.UploadSessionState;

/** Storage-owned persistent state for an in-progress upload. */
@Table("storage_upload_session")
public record UploadSessionEntity(
    @Id UUID id,
    @Column("owner_id") UUID ownerId,
    @Column("resource_id") UUID resourceId,
    String provider,
    @Column("object_key") String objectKey,
    @Column("expected_size") long expectedSize,
    @Column("declared_sha256") String declaredSha256,
    UploadSessionState state,
    @Column("expires_at") Instant expiresAt,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    Long version,
    @Column("idempotency_key") String idempotencyKey
) { }
