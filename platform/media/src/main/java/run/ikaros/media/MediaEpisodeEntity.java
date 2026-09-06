package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_episode")
public record MediaEpisodeEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("subject_id") UUID subjectId, @Column("season_id") UUID seasonId,
    @Column("resource_id") UUID resourceId, @Column("episode_number") int episodeNumber,
    @Column("absolute_number") Integer absoluteNumber, Instant airDate, Instant createdAt,
    Instant updatedAt, @Version Long version) {}
