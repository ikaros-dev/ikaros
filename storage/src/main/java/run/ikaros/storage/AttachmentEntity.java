package run.ikaros.storage;

import run.ikaros.storage.api.AttachmentKind;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Logical Attachment identity. Resource and Blob references live in link entities. */
@Table("attachment")
public record AttachmentEntity(
    @Id UUID id,
    String name,
    @Column("attachment_kind") AttachmentKind attachmentKind,
    int status,
    @Column("created_by") UUID createdBy,
    @Column("idempotency_key") String idempotencyKey,
    @Column("request_fingerprint") String requestFingerprint,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt,
    @Version Long version
) {
    /** Legacy constructor retained for source compatibility while callers migrate to link repositories. */
    @Deprecated
    public AttachmentEntity(UUID id, UUID resourceId, UUID blobId, String fileName, AttachmentKind attachmentKind,
                            Instant createdAt, Instant deletedAt, Long version) {
        this(id, fileName, attachmentKind, deletedAt == null ? 1 : 2, null, null, null,
            createdAt, latest(createdAt, deletedAt), version);
    }

    /** Legacy constructor retained for source compatibility while callers migrate to link repositories. */
    @Deprecated
    public AttachmentEntity(UUID id, UUID resourceId, UUID blobId, String fileName, AttachmentKind attachmentKind,
                            Instant createdAt, Instant deletedAt, Long version, String idempotencyKey) {
        this(id, fileName, attachmentKind, deletedAt == null ? 1 : 2, null, idempotencyKey, null,
            createdAt, latest(createdAt, deletedAt), version);
    }

    /** Legacy constructor retained for source compatibility while callers migrate to link repositories. */
    @Deprecated
    public AttachmentEntity(UUID id, UUID resourceId, UUID blobId, String fileName, AttachmentKind attachmentKind,
                            Instant createdAt, Instant deletedAt, Long version, String idempotencyKey,
                            Instant archivedAt) {
        this(id, fileName, attachmentKind, status(deletedAt, archivedAt), null, idempotencyKey, null,
            createdAt, latest(createdAt, latest(deletedAt, archivedAt)), version);
    }

    private static int status(Instant deletedAt, Instant archivedAt) {
        if (deletedAt != null) return 2;
        if (archivedAt != null) return 3;
        return 1;
    }

    private static Instant latest(Instant first, Instant second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}
