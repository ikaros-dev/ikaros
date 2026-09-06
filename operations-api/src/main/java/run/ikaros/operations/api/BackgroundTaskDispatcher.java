package run.ikaros.operations.api;

import java.time.Duration;
import reactor.core.publisher.Mono;

public interface BackgroundTaskDispatcher {
    void register(String taskType, BackgroundTaskHandler handler);
    Mono<BackgroundTask> dispatchOnce(String runnerId, Duration leaseDuration);
}
