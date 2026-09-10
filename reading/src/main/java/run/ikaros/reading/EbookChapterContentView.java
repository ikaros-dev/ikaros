package run.ikaros.reading;

import java.util.UUID;

public record EbookChapterContentView(UUID chapterId, String title, String href, String content) {}
