package run.ikaros.media;

import java.util.UUID;

public record MediaExternalSubtitleView(UUID id, UUID releaseId, UUID attachmentId, String language, String title,
    String format, String provider, long offsetMillis, boolean forced, boolean hearingImpaired) {}
