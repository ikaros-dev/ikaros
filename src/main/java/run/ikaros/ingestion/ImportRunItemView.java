package run.ikaros.ingestion;
import java.time.Instant; import java.util.UUID;
public record ImportRunItemView(UUID id,UUID runId,UUID planItemId,ImportItemStatus status,int attemptCount,String errorMessage,String idempotencyKey,Instant updatedAt) { }
