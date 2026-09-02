package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningHabitCheckInView(UUID id, UUID ownerId, UUID habitId, Double value,
    Instant occurredAt, String note, Instant createdAt) {}
