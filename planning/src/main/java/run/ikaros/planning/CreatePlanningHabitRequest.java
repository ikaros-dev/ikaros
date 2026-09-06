package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreatePlanningHabitRequest(@NotBlank String name, String description, PlanningHabitMetric metric,
    Double targetValue, @NotBlank String schedule, String timeZone, Instant startAt) {}
