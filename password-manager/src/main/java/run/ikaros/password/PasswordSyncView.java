package run.ikaros.password; import java.time.Instant; import java.util.List; public record PasswordSyncView(List<PasswordVaultItemView> items,Instant nextCursor) {}
