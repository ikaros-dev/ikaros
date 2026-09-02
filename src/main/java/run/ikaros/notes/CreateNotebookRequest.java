package run.ikaros.notes; import jakarta.validation.constraints.NotBlank; import java.util.UUID; public record CreateNotebookRequest(@NotBlank String encryptedName,UUID parentId) {}
