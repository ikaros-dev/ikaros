package run.ikaros.task;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface BackgroundTaskService {
    Mono<BackgroundTask> get(UUID taskId);
    Flux<BackgroundTask> list(TaskStatus status);
    Flux<BackgroundTaskAttemptEntity> attempts(UUID taskId);
    Mono<BackgroundTask> submit(String taskType, Map<String, Object> payload, String idempotencyKey);
    Mono<BackgroundTask> claim(String runnerId, Duration leaseDuration);
    Mono<BackgroundTask> heartbeat(UUID taskId, UUID leaseToken, Duration leaseDuration);
    Mono<BackgroundTask> complete(UUID taskId, UUID leaseToken, Map<String, Object> result);
    Mono<BackgroundTask> cancel(UUID taskId);
}
