package run.ikaros.task;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class BackgroundTaskLeaseValidationTest {
    @Test
    void heartbeatRejectsNonPositiveLeaseDuration() {
        InMemoryBackgroundTaskService service = new InMemoryBackgroundTaskService();
        assertThrows(RuntimeException.class, () -> service.heartbeat(null, null, Duration.ZERO).block());
        assertThrows(RuntimeException.class, () -> service.heartbeat(null, null, Duration.ofSeconds(-1)).block());
    }
}
