package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import run.ikaros.storage.api.AttachmentKind;

/** A query projection joining an Attachment to an optional Resource and its original Blob. */
public record AttachmentReference(
    UUID id,
    @Column("resource_id") UUID resourceId,
    @Column("blob_id") UUID blobId,
    String name,
    @Column("attachment_kind") AttachmentKind attachmentKind,
    int status,
    @Column("created_by") UUID createdBy,
    @Column("idempotency_key") String idempotencyKey,
    @Column("request_fingerprint") String requestFingerprint,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {}
