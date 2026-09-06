package run.ikaros.planning;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdatePlanningGoalProgressRequest(@NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double progress,
    long expectedVersion) {}
