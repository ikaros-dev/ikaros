package run.ikaros.media;

import java.util.UUID;

public record MediaAvailabilityResponse(UUID attachmentId, MediaContractAvailability availability,
    UUID restoreRequestId, Integer restoreLatencyMinSeconds, Integer restoreLatencyMaxSeconds,
    java.time.Instant temporaryCopyExpiresAt) {}
