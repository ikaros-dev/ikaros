package run.ikaros.music;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("music_playlist_entry") public record MusicPlaylistEntryEntity(@Id UUID id,@Column("playlist_id") UUID playlistId,@Column("track_id") UUID trackId,int position,@Column("added_at") Instant addedAt) {}
