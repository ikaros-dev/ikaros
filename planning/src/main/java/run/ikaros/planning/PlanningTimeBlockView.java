package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningTimeBlockView(UUID id, UUID ownerId, String title, UUID taskId, Instant startAt,
    Instant endAt, PlanningTimeBlockKind kind, PlanningTimeBlockStatus status, String timeZone,
    Instant createdAt, Instant updatedAt, long version) {}
