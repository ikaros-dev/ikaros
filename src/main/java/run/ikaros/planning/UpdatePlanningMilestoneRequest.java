package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record UpdatePlanningMilestoneRequest(@NotBlank String title, String description,
    Instant dueAt, long expectedVersion) {}
