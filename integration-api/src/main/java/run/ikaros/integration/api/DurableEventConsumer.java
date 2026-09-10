package run.ikaros.integration.api;

import reactor.core.publisher.Mono;

/** Public consumer extension point for durable, at-least-once event delivery. */
public interface DurableEventConsumer {
    String consumerId();

    Mono<Void> consume(DurableEvent event);
}
