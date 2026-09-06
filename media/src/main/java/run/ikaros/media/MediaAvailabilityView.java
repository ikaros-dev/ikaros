package run.ikaros.media;

import java.util.UUID;

public record MediaAvailabilityView(UUID resourceId, MediaAvailability availability,
    UUID releaseId, String reason) {}
