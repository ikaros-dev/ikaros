package run.ikaros.planning; import jakarta.validation.constraints.NotBlank; public record UpdatePlanningCommentRequest(@NotBlank String content,long expectedVersion) {}
