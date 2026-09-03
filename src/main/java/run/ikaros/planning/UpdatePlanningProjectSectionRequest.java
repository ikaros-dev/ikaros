package run.ikaros.planning; import jakarta.validation.constraints.NotBlank;
public record UpdatePlanningProjectSectionRequest(@NotBlank String name, Integer position,long expectedVersion) {}
