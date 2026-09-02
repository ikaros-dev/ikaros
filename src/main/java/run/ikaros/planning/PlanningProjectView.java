package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningProjectView(UUID id, UUID ownerId, String name, String description,
    PlanningProjectStatus status, Instant createdAt, Instant updatedAt, long version) {}
