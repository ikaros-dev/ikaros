package run.ikaros.operations.task;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import run.ikaros.common.NotFoundException;
import run.ikaros.operations.api.BackgroundTaskDispatcher;

/** Periodically claims and executes one durable background task. */
@Component
public class BackgroundTaskWorker {
    private static final Logger log = LoggerFactory.getLogger(BackgroundTaskWorker.class);
    private final BackgroundTaskDispatcher dispatcher;
    private final Duration leaseDuration;
    private final int maxConcurrency;
    private final AtomicInteger active = new AtomicInteger();
    private final String runnerId = "ikaros-worker-" + UUID.randomUUID();

    public BackgroundTaskWorker(BackgroundTaskDispatcher dispatcher,
        @Value("${ikaros.background-task.lease-duration:PT5M}") Duration leaseDuration,
        @Value("${ikaros.background-task.max-concurrency:1}") int maxConcurrency) {
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative() || maxConcurrency < 1) {
            throw new IllegalArgumentException("Background task worker limits must be positive");
        }
        this.dispatcher = dispatcher;
        this.leaseDuration = leaseDuration;
        this.maxConcurrency = maxConcurrency;
    }

    @Scheduled(fixedDelayString = "${ikaros.background-task.worker-delay-ms:1000}",
        initialDelayString = "${ikaros.background-task.worker-initial-delay-ms:3000}")
    public void dispatchOne() {
        if (active.incrementAndGet() > maxConcurrency) {
            active.decrementAndGet();
            return;
        }
        dispatcher.dispatchOnce(runnerId, leaseDuration)
            .onErrorResume(NotFoundException.class, ignored -> reactor.core.publisher.Mono.empty())
            .doOnError(error -> log.warn("Background task worker failed: {}", error.getMessage()))
            .doFinally(signal -> active.decrementAndGet())
            .subscribe(ignored -> { }, ignored -> { });
    }
}
