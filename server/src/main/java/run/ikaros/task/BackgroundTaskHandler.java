package run.ikaros.task;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface BackgroundTaskHandler {
    Mono<java.util.Map<String, Object>> handle(BackgroundTask task);
}
