package run.ikaros.event;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEventConsumer;
import run.ikaros.integration.api.DurableEvent;
import run.ikaros.common.NotFoundException;
import java.util.UUID;
import reactor.core.publisher.Flux;

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
            .doOnError(error -> log.warn("Durable event dispatcher failed", error))
            .subscribe(ignored -> { }, ignored -> { });
    }

    Mono<Void> dispatchNow() {
        return Flux.fromIterable(consumers)
            .concatMap(events::dispatchOnce)
            .then();
    }

    public Flux<DurableEvent> pendingEvents() {
        return events.pendingEvents();
    }

    public Mono<Long> retry(UUID eventId) {
        if (consumers.isEmpty()) {
            return Mono.error(new NotFoundException("没有注册事件 Consumer"));
        }
        return Flux.fromIterable(consumers)
            .concatMap(consumer -> events.dispatchOnce(eventId, consumer))
            .reduce(0L, Long::sum);
    }
}
