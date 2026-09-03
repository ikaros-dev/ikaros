package run.ikaros.media;

import java.util.UUID;

public record MediaEpisodeView(UUID id, UUID subjectId, UUID seasonId, UUID resourceId,
    int episodeNumber, Integer absoluteNumber) {}
