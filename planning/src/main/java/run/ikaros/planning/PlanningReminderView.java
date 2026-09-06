package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningReminderView(UUID id, UUID ownerId, PlanningReminderTargetType targetType,
    UUID targetId, Instant triggerAt, String timeZone, String channel, PlanningReminderStatus status,
    Instant snoozedUntil, Instant firedAt, Instant acknowledgedAt, Instant createdAt, Instant updatedAt,
    long version) {}
