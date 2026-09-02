package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;

public record PlaybackSessionView(UUID id, UUID resourceId, UUID releaseId, PlaybackSessionState state,
    Instant startedAt, Instant endedAt, long lastPositionSeconds) {}
