package run.ikaros.operations.api;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface BackgroundTaskService {
    Mono<BackgroundTask> get(UUID taskId);
    Mono<BackgroundTask> findByTaskTypeAndIdempotencyKey(String taskType, String idempotencyKey);
    Mono<BackgroundTask> submit(String taskType, Map<String, Object> payload, String idempotencyKey);
    Mono<BackgroundTask> claim(String runnerId, Duration leaseDuration);
    Mono<BackgroundTask> heartbeat(UUID taskId, UUID leaseToken, Duration leaseDuration);
    Mono<BackgroundTask> updateProgress(UUID taskId, UUID leaseToken, Map<String, Object> progress);
    Mono<BackgroundTask> complete(UUID taskId, UUID leaseToken, Map<String, Object> result);
    Mono<BackgroundTask> fail(UUID taskId, UUID leaseToken, Map<String, Object> error);
    Mono<BackgroundTask> retry(UUID taskId);
    Mono<BackgroundTask> cancel(UUID taskId);
    Mono<BackgroundTask> acknowledgeCancellation(UUID taskId, UUID leaseToken);
}
