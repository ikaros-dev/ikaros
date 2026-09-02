package run.ikaros.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BackgroundTaskDispatcherTest {
    @Test
    void claimsRunsAndCompletesRegisteredHandler() {
        InMemoryBackgroundTaskService tasks = new InMemoryBackgroundTaskService();
        BackgroundTaskDispatcher dispatcher = new BackgroundTaskDispatcher(tasks);
        dispatcher.register("test", task -> reactor.core.publisher.Mono.just(Map.of("ok", true)));
        tasks.submit("test", Map.of(), "once").block();
        BackgroundTask result = dispatcher.dispatchOnce("runner", Duration.ofMinutes(1)).block();
        assertEquals(TaskStatus.SUCCEEDED, result.status());
    }
}
