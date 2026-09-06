package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreatePlanningRecurrenceRequest(@NotBlank String rule, PlanningRecurrenceMode mode,
    String timeZone, Instant nextRunAt) {}
