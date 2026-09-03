package run.ikaros.notes; import jakarta.validation.constraints.NotBlank; public record CreatePrivateTagRequest(@NotBlank String encryptedName) {}
