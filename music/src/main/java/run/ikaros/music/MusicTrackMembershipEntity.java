package run.ikaros.music;
import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("music_track_membership") public record MusicTrackMembershipEntity(@Id UUID id,@Column("edition_id") UUID editionId,@Column("disc_id") UUID discId,@Column("track_id") UUID trackId,@Column("track_number") int trackNumber,@Column("sort_order") int sortOrder,@Column("title_override") String titleOverride,@Column("duration_millis") Long durationMillis,boolean hidden) {}
