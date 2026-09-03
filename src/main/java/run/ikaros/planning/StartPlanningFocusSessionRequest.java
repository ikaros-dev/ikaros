package run.ikaros.planning;

import jakarta.validation.constraints.Min;
import java.util.UUID;

public record StartPlanningFocusSessionRequest(UUID taskId, PlanningFocusMode mode, @Min(1) Integer plannedMinutes) {}
