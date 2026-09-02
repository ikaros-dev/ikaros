package run.ikaros.finance; import java.time.Instant; import java.util.UUID; public record TransactionTagView(UUID id,UUID transactionId,UUID tagId,Instant createdAt) {}
