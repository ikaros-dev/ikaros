package run.ikaros.notes; import java.time.Instant; import java.util.UUID; public record PrivateAttachmentView(UUID id,UUID noteId,UUID attachmentId,String encryptedFileName,Instant createdAt) {}
