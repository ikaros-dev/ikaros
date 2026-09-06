package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningFocusSessionView(UUID id, UUID ownerId, UUID taskId, PlanningFocusMode mode,
    PlanningFocusSessionStatus status, Integer plannedMinutes, Integer actualMinutes, Instant startedAt,
    Instant endedAt, String note, Instant createdAt, long version) {}
