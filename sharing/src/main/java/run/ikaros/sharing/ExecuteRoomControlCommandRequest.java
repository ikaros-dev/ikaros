package run.ikaros.sharing; import jakarta.validation.constraints.NotBlank; public record ExecuteRoomControlCommandRequest(@NotBlank String commandType,String payload,Long expectedStateVersion) {}
