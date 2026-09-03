package run.ikaros.notes; import java.time.Instant; import java.util.UUID; public record NotebookView(UUID id,UUID vaultId,UUID parentId,String encryptedName,Instant createdAt) {}
