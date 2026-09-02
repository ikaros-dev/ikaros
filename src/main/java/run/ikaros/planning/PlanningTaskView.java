package run.ikaros.planning;
import java.time.Instant;
import java.util.UUID;
public record PlanningTaskView(UUID id, UUID ownerId, String title, String description,
    PlanningTaskStatus status, PlanningTaskPriority priority, Instant scheduledStart, Instant scheduledEnd,
    Instant deadline, UUID projectId,
    UUID parentTaskId, Instant completedAt, Instant createdAt, Instant updatedAt, long version) {}
