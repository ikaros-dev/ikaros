package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Binding between one logical Attachment and one immutable Blob representation. */
@Table("attachment_blob")
public record AttachmentBlobEntity(
    @Id UUID id,
    @Column("attachment_id") UUID attachmentId,
    @Column("blob_id") UUID blobId,
    String role,
    @Column("created_at") Instant createdAt,
    @Version Long version
) {
    public static final String ORIGINAL = "ORIGINAL";
    public static final String DERIVED = "DERIVED";
}
