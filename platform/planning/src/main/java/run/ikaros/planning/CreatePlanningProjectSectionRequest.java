package run.ikaros.planning; import jakarta.validation.constraints.NotBlank;
public record CreatePlanningProjectSectionRequest(@NotBlank String name, Integer position) {}
