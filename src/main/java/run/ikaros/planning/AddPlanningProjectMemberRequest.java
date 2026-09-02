package run.ikaros.planning;
import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record AddPlanningProjectMemberRequest(@NotNull UUID userId,@NotNull PlanningProjectMemberRole role) {}
