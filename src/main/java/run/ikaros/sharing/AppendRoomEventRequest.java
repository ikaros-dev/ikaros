package run.ikaros.sharing; import jakarta.validation.constraints.NotBlank; public record AppendRoomEventRequest(@NotBlank String eventType,String payload,Long expectedStateVersion) {}
