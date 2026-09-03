package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;

public record UpdatePlanningProjectRequest(@NotBlank String name, String description, Long expectedVersion) {}
