package run.ikaros.planning;
import java.time.Instant;
import java.util.UUID;
public record PlanningTaskView(UUID id, UUID ownerId, String title, String description,
    PlanningTaskStatus status, PlanningTaskPriority priority, boolean important, boolean urgent, Instant scheduledStart, Instant scheduledEnd,
    Instant deadline, Integer estimatedDurationMinutes, UUID projectId, UUID sectionId,
    UUID parentTaskId, Instant completedAt, Instant createdAt, Instant updatedAt, long version) {}
