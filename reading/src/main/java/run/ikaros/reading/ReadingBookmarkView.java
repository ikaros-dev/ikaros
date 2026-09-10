package run.ikaros.reading;

import java.time.Instant;
import java.util.UUID;

public record ReadingBookmarkView(UUID id, UUID workId, UUID editionId, UUID chapterId,
    String locatorKind, String locatorValue, String contentVersion, String label, Instant createdAt) {}
