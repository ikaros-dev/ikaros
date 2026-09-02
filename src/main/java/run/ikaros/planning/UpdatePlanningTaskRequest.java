package run.ikaros.planning;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
public record UpdatePlanningTaskRequest(@NotBlank String title, String description,
    PlanningTaskPriority priority, Instant deadline, long expectedVersion) {}
