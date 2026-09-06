package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_subject")
public record MediaSubjectEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("resource_id") UUID resourceId, MediaSubjectKind kind, Instant createdAt,
    Instant updatedAt, @Version Long version) {}
