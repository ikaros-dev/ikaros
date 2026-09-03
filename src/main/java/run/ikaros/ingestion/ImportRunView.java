package run.ikaros.ingestion;
import java.time.Instant;
import java.util.UUID;
public record ImportRunView(UUID id, UUID planId, UUID actorId, ImportRunStatus status, String checkpoint,
    long completedCount, long failedCount, long skippedCount, UUID backgroundTaskId, Instant startedAt,
    Instant finishedAt, Instant createdAt) { }
