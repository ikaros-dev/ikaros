package run.ikaros.music;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("music_playback_history") public record MusicPlaybackHistoryEntity(@Id UUID id,@Column("owner_id") UUID ownerId,@Column("track_id") UUID trackId,@Column("source_id") UUID sourceId,@Column("started_at") Instant startedAt,@Column("ended_at") Instant endedAt,@Column("listened_millis") long listenedMillis,MusicPlaybackState outcome) {}
