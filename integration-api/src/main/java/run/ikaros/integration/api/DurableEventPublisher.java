package run.ikaros.integration.api;

import reactor.core.publisher.Mono;

/** Durable Event 的公开发布契约；实现必须将事件写入事务内 Outbox。 */
public interface DurableEventPublisher {
    Mono<EventReference> append(EventAppendRequest request);
}
