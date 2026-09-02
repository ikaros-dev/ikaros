package run.ikaros.task;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BackgroundTask(UUID id, String taskType, TaskStatus status, Map<String, Object> payload,
                             String idempotencyKey, Instant availableAt, String leaseOwner,
                             UUID leaseToken, Instant leaseExpiresAt, int attempt, Instant cancelRequestedAt,
                             Map<String, Object> progress, Map<String, Object> result,
                             Instant createdAt, Instant updatedAt, UUID parentTaskId) {
    public BackgroundTask {
        payload = Map.copyOf(payload == null ? Map.of() : payload);
        progress = Map.copyOf(progress == null ? Map.of() : progress);
        result = Map.copyOf(result == null ? Map.of() : result);
    }
}
