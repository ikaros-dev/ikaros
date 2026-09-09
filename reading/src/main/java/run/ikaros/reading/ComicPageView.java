package run.ikaros.reading;

import java.util.UUID;

public record ComicPageView(UUID id, UUID chapterId, UUID attachmentId, int pageOrder,
    String pageRole, Integer width, Integer height, String spreadHint) {}
