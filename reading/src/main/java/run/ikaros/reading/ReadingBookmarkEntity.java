package run.ikaros.reading;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("reading_bookmark")
public record ReadingBookmarkEntity(@Id UUID id, @Column("owner_id") UUID ownerId,
    @Column("work_id") UUID workId, @Column("edition_id") UUID editionId,
    @Column("chapter_id") UUID chapterId, @Column("locator_kind") String locatorKind,
    @Column("locator_value") String locatorValue, @Column("content_version") String contentVersion,
    String label, @Column("created_at") Instant createdAt) {}
