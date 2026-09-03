package run.ikaros.music;
import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("music_disc") public record MusicDiscEntity(@Id UUID id,@Column("edition_id") UUID editionId,@Column("disc_number") int discNumber,String title,@Column("medium_format") String mediumFormat,int position) {}
