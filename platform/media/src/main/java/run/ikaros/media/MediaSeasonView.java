package run.ikaros.media;

import java.util.UUID;

public record MediaSeasonView(UUID id, UUID subjectId, UUID resourceId, int seasonNumber, String name) {}
