package run.ikaros.game; import jakarta.validation.constraints.NotBlank; public record CreateGameRequest(@NotBlank String title,String gameKind,String locale) {}
