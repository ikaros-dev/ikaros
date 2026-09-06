package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningRecurrenceView(UUID id, UUID ownerId, UUID taskId, String rule,
    PlanningRecurrenceMode mode, String timeZone, Instant nextRunAt, boolean active,
    Instant lastRunAt, Instant createdAt, Instant updatedAt, long version) {}
