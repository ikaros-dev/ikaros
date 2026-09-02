package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningGoalView(UUID id, UUID ownerId, String title, String description, PlanningGoalType type,
    PlanningGoalStatus status, double progress, Instant startAt, Instant deadline, Instant completedAt,
    Instant createdAt, Instant updatedAt, long version) {}
