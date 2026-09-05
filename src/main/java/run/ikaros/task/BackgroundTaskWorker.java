package run.ikaros.task;

import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import run.ikaros.common.NotFoundException;

/** Periodically claims and executes one durable background task. */
@Component
public class BackgroundTaskWorker {
    private static final Logger log = LoggerFactory.getLogger(BackgroundTaskWorker.class);
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);
    private final BackgroundTaskDispatcher dispatcher;
    private final String runnerId = "ikaros-worker-" + UUID.randomUUID();

    public BackgroundTaskWorker(BackgroundTaskDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${ikaros.background-task.worker-delay-ms:1000}",
        initialDelayString = "${ikaros.background-task.worker-initial-delay-ms:3000}")
    public void dispatchOne() {
        dispatcher.dispatchOnce(runnerId, LEASE_DURATION)
            .onErrorResume(NotFoundException.class, ignored -> reactor.core.publisher.Mono.empty())
            .doOnError(error -> log.warn("Background task worker failed: {}", error.getMessage()))
            .subscribe(ignored -> { }, ignored -> { });
    }
}
