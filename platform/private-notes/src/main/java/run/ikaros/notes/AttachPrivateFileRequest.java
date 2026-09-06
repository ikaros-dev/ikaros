package run.ikaros.notes; import jakarta.validation.constraints.NotNull; import java.util.UUID; public record AttachPrivateFileRequest(@NotNull UUID attachmentId,String encryptedFileName) {}
