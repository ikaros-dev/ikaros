package run.ikaros.planning;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
public record CreatePlanningTaskRequest(@NotBlank String title, String description,
    PlanningTaskPriority priority, boolean important, boolean urgent, Instant scheduledStart, Instant scheduledEnd, Instant deadline,
    Integer estimatedDurationMinutes,
    UUID projectId, UUID sectionId, UUID parentTaskId) {}
