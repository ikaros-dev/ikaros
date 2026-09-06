package run.ikaros.game; import jakarta.validation.constraints.NotBlank; public record CreateGamePlatformRequest(@NotBlank String name,String family,String architecture) {}
