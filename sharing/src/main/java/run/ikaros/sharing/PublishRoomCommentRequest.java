package run.ikaros.sharing; import jakarta.validation.constraints.NotBlank; public record PublishRoomCommentRequest(@NotBlank String body,@NotBlank String idempotencyKey) {}
