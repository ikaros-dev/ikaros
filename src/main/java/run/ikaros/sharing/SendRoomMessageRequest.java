package run.ikaros.sharing; import jakarta.validation.constraints.NotBlank; public record SendRoomMessageRequest(@NotBlank String body,@NotBlank String idempotencyKey) {}
