package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("media_playback_history")
public record MediaPlaybackHistoryEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("resource_id") UUID resourceId, @Column("session_id") UUID sessionId,
    @Column("started_at") Instant startedAt, @Column("ended_at") Instant endedAt,
    @Column("watched_seconds") long watchedSeconds) {}
