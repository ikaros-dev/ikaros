package run.ikaros.planning; import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record AssignPlanningTaskRequest(@NotNull UUID assigneeId) {}
