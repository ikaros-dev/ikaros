package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;

public record MediaReleaseView(UUID id, UUID playableResourceId, UUID attachmentId, String releaseGroup,
    String versionLabel, MediaReleaseState state, String contentFingerprint, Instant createdAt, Instant updatedAt) {}
