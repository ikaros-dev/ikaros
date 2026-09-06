package run.ikaros.planning;

import jakarta.validation.constraints.NotBlank;

public record CreatePlanningTagRequest(@NotBlank String name, String color) {}
