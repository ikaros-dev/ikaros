package run.ikaros.music;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.annotation.Version; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("music_album") public record MusicAlbumEntity(@Id UUID id,@Column("owner_id") UUID ownerId,@Column("resource_id") UUID resourceId,String kind,@Column("original_release_date") Instant originalReleaseDate,@Version Long version) {}
