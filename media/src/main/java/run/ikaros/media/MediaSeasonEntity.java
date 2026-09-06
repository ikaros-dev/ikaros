package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_season")
public record MediaSeasonEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("subject_id") UUID subjectId, @Column("resource_id") UUID resourceId,
    @Column("season_number") int seasonNumber, String name, Instant createdAt,
    Instant updatedAt, @Version Long version) {}
