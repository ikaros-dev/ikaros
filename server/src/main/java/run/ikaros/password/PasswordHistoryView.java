package run.ikaros.password; import java.time.Instant; import java.util.UUID; public record PasswordHistoryView(UUID id,UUID itemId,long revision,String encryptedPayload,Instant createdAt) {}
