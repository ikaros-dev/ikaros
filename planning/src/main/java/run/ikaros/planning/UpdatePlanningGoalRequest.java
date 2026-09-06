package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record UpdatePlanningGoalRequest(@NotBlank String title, String description, PlanningGoalType type,
    Instant startAt, Instant deadline, long expectedVersion) {}
