package run.ikaros.reading;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("reading_ebook_import")
public record EbookImportEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("source_attachment_id") UUID sourceAttachmentId, @Column("work_id") UUID workId,
    @Column("edition_id") UUID editionId, String status, @Column("error_code") String errorCode,
    @Column("error_message") String errorMessage, @Column("idempotency_key") String idempotencyKey,
    @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt,
    @Version Long version) {}
