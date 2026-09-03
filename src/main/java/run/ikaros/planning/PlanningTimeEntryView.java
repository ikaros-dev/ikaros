package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningTimeEntryView(UUID id, UUID ownerId, UUID taskId, int durationMinutes,
    Instant startedAt, Instant endedAt, PlanningTimeEntrySource source, String note, Instant createdAt) {}
