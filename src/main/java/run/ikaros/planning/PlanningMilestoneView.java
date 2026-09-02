package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningMilestoneView(UUID id, UUID ownerId, String title, String description,
    UUID goalId, UUID projectId, Instant dueAt, PlanningMilestoneStatus status, Instant achievedAt,
    Instant createdAt, Instant updatedAt, long version) {}
