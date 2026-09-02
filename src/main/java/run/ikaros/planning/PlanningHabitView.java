package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningHabitView(UUID id, UUID ownerId, String name, String description, PlanningHabitMetric metric,
    Double targetValue, String schedule, String timeZone, Instant startAt, PlanningHabitStatus status,
    Instant createdAt, Instant updatedAt, long version) {}
