package run.ikaros.reading;
import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.relational.core.mapping.Table; import org.springframework.data.relational.core.mapping.Column;
@Table("reading_comic_page") public record ReadingComicPageEntity(@Id UUID id,@Column("chapter_id") UUID chapterId,@Column("attachment_id") UUID attachmentId,@Column("page_order") int pageOrder,@Column("page_role") String pageRole,Integer width,Integer height,@Column("spread_hint") String spreadHint) {}
