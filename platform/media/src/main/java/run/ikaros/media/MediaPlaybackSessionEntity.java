package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_playback_session")
public record MediaPlaybackSessionEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("resource_id") UUID resourceId, @Column("release_id") UUID releaseId,
    PlaybackSessionState state, @Column("started_at") Instant startedAt, @Column("ended_at") Instant endedAt,
    @Column("last_position_seconds") long lastPositionSeconds, @Version Long version) {}
