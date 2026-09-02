package run.ikaros.planning;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
public record CreatePlanningTaskRequest(@NotBlank String title, String description,
    PlanningTaskPriority priority, Instant scheduledStart, Instant scheduledEnd, Instant deadline,
    Integer estimatedDurationMinutes,
    UUID projectId, UUID parentTaskId) {}
