package run.ikaros.reading;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("reading_ebook_chapter")
public record EbookChapterEntity(@Id UUID id, @Column("import_id") UUID importId,
    @Column("chapter_id") UUID chapterId, String href, String title,
    @Column("sort_order") int sortOrder) {}
