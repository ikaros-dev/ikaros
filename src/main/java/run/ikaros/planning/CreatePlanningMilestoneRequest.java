package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreatePlanningMilestoneRequest(@NotBlank String title, String description,
    @NotNull UUID goalId, UUID projectId, Instant dueAt) {}
