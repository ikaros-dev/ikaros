package run.ikaros.password; import jakarta.validation.constraints.NotBlank; import java.util.UUID; public record CreatePasswordFolderRequest(@NotBlank String encryptedName,UUID parentId) {}
