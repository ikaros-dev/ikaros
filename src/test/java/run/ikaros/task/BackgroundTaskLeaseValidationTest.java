package run.ikaros.task;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class BackgroundTaskLeaseValidationTest {
    @Test
    void heartbeatRejectsNonPositiveLeaseDuration() {
        InMemoryBackgroundTaskService service = new InMemoryBackgroundTaskService();
        assertThrows(RuntimeException.class, () -> service.heartbeat(null, null, Duration.ZERO).block());
        assertThrows(RuntimeException.class, () -> service.heartbeat(null, null, Duration.ofSeconds(-1)).block());
    }

    @Test
    void unpagedListIsBounded() {
        InMemoryBackgroundTaskService service = new InMemoryBackgroundTaskService();
        for (int i = 0; i < 101; i++) {
            service.submit("task", java.util.Map.of("index", i), null).block();
        }
        StepVerifier.create(service.list(null).count())
            .expectNext(100L)
            .verifyComplete();
    }

    @Test
    void submitNormalizesTaskType() {
        InMemoryBackgroundTaskService service = new InMemoryBackgroundTaskService();
        BackgroundTask task = service.submit("  import  ", Map.of(), "same-key").block();
        BackgroundTask reused = service.submit("import", Map.of("different", true), "same-key").block();
        org.junit.jupiter.api.Assertions.assertEquals("import", task.taskType());
        org.junit.jupiter.api.Assertions.assertEquals(task.id(), reused.id());
    }
}
