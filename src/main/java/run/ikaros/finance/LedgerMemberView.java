package run.ikaros.finance; import java.time.Instant; import java.util.UUID; public record LedgerMemberView(UUID id,UUID ledgerId,UUID principalId,LedgerMemberRole role,Instant createdAt) {}
