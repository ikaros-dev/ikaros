package run.ikaros.media;

import java.util.UUID;

public record PlaybackSourceView(UUID releaseId, UUID attachmentId, PlaybackSourceMode mode,
    String reason, String contentUrl) {}
