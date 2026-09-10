package run.ikaros.reading;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("reading_comic_import_entry")
public record ComicImportEntryEntity(@Id UUID id, @Column("import_id") UUID importId,
    @Column("chapter_key") String chapterKey, @Column("entry_name") String entryName,
    @Column("page_order") int pageOrder, @Column("page_role") String pageRole, @Version Long version) {}
