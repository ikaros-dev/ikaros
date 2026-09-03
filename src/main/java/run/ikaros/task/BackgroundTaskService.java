package run.ikaros.task;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import run.ikaros.common.PageResponse;

public interface BackgroundTaskService {
    Mono<BackgroundTask> get(UUID taskId);
    Flux<BackgroundTask> list(TaskStatus status);
    Mono<PageResponse<BackgroundTask>> list(TaskStatus status, String taskType, int page, int size);
    Flux<BackgroundTaskAttemptEntity> attempts(UUID taskId);
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
