package run.ikaros.music;
import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Column; import org.springframework.data.relational.core.mapping.Table;
@Table("music_track_artist") public record MusicTrackArtistEntity(@Id UUID id,@Column("owner_id") UUID ownerId,@Column("track_id") UUID trackId,@Column("artist_id") UUID artistId,String role,int position) {}
