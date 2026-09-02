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

    @org.junit.jupiter.api.Test
    void persistsHandlerFailureBeforePropagatingError() {
        InMemoryBackgroundTaskService tasks = new InMemoryBackgroundTaskService();
        BackgroundTaskDispatcher dispatcher = new BackgroundTaskDispatcher(tasks);
        dispatcher.register("broken", task -> reactor.core.publisher.Mono.error(new IllegalArgumentException("boom")));
        BackgroundTask submitted = tasks.submit("broken", Map.of(), "broken-once").block();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> dispatcher.dispatchOnce("runner", Duration.ofMinutes(1)).block());
        assertEquals(TaskStatus.FAILED, tasks.get(submitted.id()).block().status());
    }

    @org.junit.jupiter.api.Test
    void retryableHandlerFailureReturnsTaskToPendingWithBackoff() {
        InMemoryBackgroundTaskService tasks = new InMemoryBackgroundTaskService();
        BackgroundTaskDispatcher dispatcher = new BackgroundTaskDispatcher(tasks);
        dispatcher.register("transient", task -> reactor.core.publisher.Mono.error(new RuntimeException("temporary")));
        BackgroundTask submitted = tasks.submit("transient", Map.of(), "transient-once").block();
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> dispatcher.dispatchOnce("runner", Duration.ofMinutes(1)).block());
        assertEquals(TaskStatus.PENDING, tasks.get(submitted.id()).block().status());
    }

    @org.junit.jupiter.api.Test
    void manualRetryCreatesChildPendingTask() {
        InMemoryBackgroundTaskService tasks = new InMemoryBackgroundTaskService();
        BackgroundTask submitted = tasks.submit("broken", Map.of(), "original").block();
        BackgroundTask running = tasks.claim("runner", Duration.ofMinutes(1)).block();
        tasks.fail(running.id(), running.leaseToken(), Map.of("code", "BOOM")).block();
        BackgroundTask retry = tasks.retry(submitted.id()).block();
        assertEquals(TaskStatus.PENDING, retry.status());
        assertEquals(submitted.id(), retry.parentTaskId());
    }

    @org.junit.jupiter.api.Test
    void expiredLeaseIsReclaimedOnNextClaim() throws InterruptedException {
        InMemoryBackgroundTaskService tasks = new InMemoryBackgroundTaskService();
        tasks.submit("recoverable", Map.of(), "lease-recovery").block();
        tasks.claim("crashed-runner", Duration.ofMillis(1)).block();
        Thread.sleep(10);
        BackgroundTask reclaimed = tasks.claim("healthy-runner", Duration.ofMinutes(1)).block();
        assertEquals(TaskStatus.RUNNING, reclaimed.status());
        assertEquals(2, reclaimed.attempt());
    }

    @org.junit.jupiter.api.Test
    void cancellingRunningTaskOnlyRequestsCancellation() {
        InMemoryBackgroundTaskService tasks = new InMemoryBackgroundTaskService();
        tasks.submit("cancellable", Map.of(), "cancel-running").block();
        BackgroundTask running = tasks.claim("runner", Duration.ofMinutes(1)).block();
        BackgroundTask requested = tasks.cancel(running.id()).block();
        assertEquals(TaskStatus.RUNNING, requested.status());
        org.junit.jupiter.api.Assertions.assertNotNull(requested.cancelRequestedAt());
    }

    @org.junit.jupiter.api.Test
    void expiredTaskIsNotClaimedAndBecomesTimedOut() throws InterruptedException {
        InMemoryBackgroundTaskService tasks = new InMemoryBackgroundTaskService();
        BackgroundTask submitted = tasks.submit("short", Map.of("timeout_seconds", 1), "short-timeout").block();
        Thread.sleep(1100);
        org.junit.jupiter.api.Assertions.assertNull(tasks.claim("runner", Duration.ofMinutes(1)).block());
        assertEquals(TaskStatus.TIMED_OUT, tasks.get(submitted.id()).block().status());
    }
}
