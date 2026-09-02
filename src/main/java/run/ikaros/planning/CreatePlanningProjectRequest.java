package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;

public record CreatePlanningProjectRequest(@NotBlank String name, String description) {}
