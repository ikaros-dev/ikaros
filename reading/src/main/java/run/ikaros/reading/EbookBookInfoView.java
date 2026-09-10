package run.ikaros.reading;

import java.util.UUID;

public record EbookBookInfoView(UUID importId, UUID workId, UUID editionId, String title,
    String language, String publisher, String source, long chapterCount,
    ComicImportStatus importStatus, String errorCode, String errorMessage) {}
