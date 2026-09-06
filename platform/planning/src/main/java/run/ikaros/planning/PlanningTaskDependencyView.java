package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningTaskDependencyView(UUID id, UUID taskId, UUID dependsOnTaskId,
    PlanningTaskDependencyType type, Instant createdAt) {}
