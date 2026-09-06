package run.ikaros.planning; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record CreatePlanningCommentRequest(@NotNull PlanningCommentTargetType targetType,@NotNull UUID targetId,@NotBlank String content) {}
