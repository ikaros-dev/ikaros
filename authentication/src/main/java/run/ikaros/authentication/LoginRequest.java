package run.ikaros.authentication; import jakarta.validation.constraints.NotBlank; public record LoginRequest(@NotBlank String username,@NotBlank String password) {}
