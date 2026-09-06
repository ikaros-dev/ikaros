package run.ikaros.operations.api;

import java.util.Map;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface BackgroundTaskHandler {
    Mono<Map<String, Object>> handle(BackgroundTask task);
}
