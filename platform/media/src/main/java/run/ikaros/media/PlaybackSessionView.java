package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;

public record PlaybackSessionView(UUID id, UUID resourceId, UUID releaseId, PlaybackSessionState state,
    Instant startedAt, Instant endedAt, long lastPositionSeconds, Long version) {
    public PlaybackSessionView(UUID id, UUID resourceId, UUID releaseId, PlaybackSessionState state,
        Instant startedAt, Instant endedAt, long lastPositionSeconds) {
        this(id, resourceId, releaseId, state, startedAt, endedAt, lastPositionSeconds, null);
    }
}
