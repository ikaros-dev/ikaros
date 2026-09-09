package run.ikaros.reading;

import java.util.UUID;

public record EbookChapterView(UUID id, UUID importId, UUID chapterId, String href,
    String title, int sortOrder) {}
