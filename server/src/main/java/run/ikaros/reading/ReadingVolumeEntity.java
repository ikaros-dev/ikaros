package run.ikaros.reading;
import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("reading_volume") public record ReadingVolumeEntity(@Id UUID id,@Column("edition_id") UUID editionId,String kind,@Column("display_label") String displayLabel,@Column("structured_number") String structuredNumber,@Column("sort_order") int sortOrder,String title,@Column("cover_attachment_id") UUID coverAttachmentId) {}
