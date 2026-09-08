package run.ikaros.event;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEventConsumer;

/** Re-scans the durable Outbox after startup so pending events survive process restarts. */
@Component
public final class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final DurableEventService events;
    private final List<DurableEventConsumer> consumers;

    public OutboxDispatcher(DurableEventService events, List<DurableEventConsumer> consumers) {
        this.events = events;
        this.consumers = consumers;
    }

    @Scheduled(fixedDelayString = "${ikaros.event.dispatcher-delay-ms:1000}",
        initialDelayString = "${ikaros.event.dispatcher-initial-delay-ms:3000}")
    public void dispatchPendingEvents() {
        dispatchNow()
            .doOnError(error -> log.warn("Durable event dispatcher failed: {}", error.getMessage()))
            .subscribe(ignored -> { }, ignored -> { });
    }

    Mono<Void> dispatchNow() {
        return reactor.core.publisher.Flux.fromIterable(consumers)
            .concatMap(events::dispatchOnce)
            .then();
    }
}
