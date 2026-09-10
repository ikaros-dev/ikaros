package run.ikaros.music;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("music_import")
public record MusicImportEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("attachment_id") UUID attachmentId, @Column("track_id") UUID trackId,
    String status, @Column("error_code") String errorCode, @Column("error_message") String errorMessage,
    @Column("idempotency_key") String idempotencyKey, @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt, @Version Long version) {}
