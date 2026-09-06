package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;

public record PlaybackHistoryView(UUID id, UUID resourceId, UUID sessionId, Instant startedAt, Instant endedAt,
    long watchedSeconds) {}
