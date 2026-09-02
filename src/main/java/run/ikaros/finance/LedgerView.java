package run.ikaros.finance; import java.time.Instant; import java.util.UUID; public record LedgerView(UUID id,UUID ownerId,String name,String baseCurrency,boolean archived,Instant createdAt) {}
