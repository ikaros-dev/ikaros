package run.ikaros.ingestion;

import java.time.Instant;
import java.util.UUID;

public record ScanRunView(UUID id, UUID sourceId, String trigger, UUID actorId, ScanRunStatus status,
                          String checkpoint, long discoveredCount, long changedCount, long skippedCount,
                          String errorSummary, UUID backgroundTaskId, Instant startedAt, Instant finishedAt,
                          Instant createdAt) { }
