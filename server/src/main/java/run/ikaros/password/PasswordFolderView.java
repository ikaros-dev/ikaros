package run.ikaros.password; import java.time.Instant; import java.util.UUID; public record PasswordFolderView(UUID id,UUID vaultId,UUID parentId,String encryptedName,Instant createdAt) {}
