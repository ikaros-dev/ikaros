package run.ikaros.event;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import run.ikaros.common.PrincipalContext;
import run.ikaros.common.PrincipalContexts;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.DurableEventConsumer;
import run.ikaros.integration.api.DurableEvent;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.integration.api.EventReference;

/** Outbox 写入与 Inbox 幂等消费边界。 */
@Service
public class DurableEventService implements DurableEventPublisher {
    private static final int MAX_DELIVERY_ATTEMPTS = 8;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final java.util.regex.Pattern EVENT_TYPE =
        java.util.regex.Pattern.compile("[a-z][a-z0-9-]*\\.[a-z][a-z0-9-]*\\.[a-z][a-z0-9-]*");
    private final OutboxEventRepository outbox;
    private final InboxEntryRepository inbox;
    private final OutboxDeliveryRepository deliveries;
    private final TransactionalOperator transaction;

    public DurableEventService(OutboxEventRepository outbox, InboxEntryRepository inbox,
                               OutboxDeliveryRepository deliveries,
                               TransactionalOperator transaction) {
        this.outbox = outbox;
        this.inbox = inbox;
        this.deliveries = deliveries;
        this.transaction = transaction;
    }

    @Override
    public Mono<EventReference> append(EventAppendRequest request) {
        if (request == null || request.producerSubsystem() == null || request.producerSubsystem().isBlank()
            || request.subjectType() == null || request.subjectType().isBlank()) {
            return Mono.error(new IllegalArgumentException("事件 Producer 和 Subject 不合法"));
        }
        return appendValidated(request.eventType(), request.schemaVersion(), request.producerSubsystem(),
            request.subjectType(), request.subjectId(), request.payloadJson())
            .map(saved -> new EventReference(saved.id(), saved.eventType(), saved.schemaVersion()));
    }

    private Mono<OutboxEventEntity> appendValidated(String eventType, int schemaVersion, String producerSubsystem,
                                                    String subjectType, UUID subjectId, String payloadJson) {
        if (eventType == null || !EVENT_TYPE.matcher(eventType).matches() || schemaVersion < 1 || payloadJson == null) {
            return Mono.error(new IllegalArgumentException("事件类型、版本和 Payload 不合法"));
        }
        try {
            JsonNode payload = JSON.readTree(payloadJson);
            if (payload == null || !payload.isObject()) {
                return Mono.error(new IllegalArgumentException("事件 Payload 必须是 JSON object"));
            }
        } catch (JacksonException exception) {
            return Mono.error(new IllegalArgumentException("事件 Payload 必须是合法 JSON", exception));
        }
        String normalized = payloadJson.toLowerCase();
        if (normalized.contains("password") || normalized.contains("secret")
            || normalized.contains("private_key") || normalized.contains("access_token")
            || normalized.contains("refresh_token")) {
            return Mono.error(new IllegalArgumentException("事件 Payload 不得包含 Secret 或 Token"));
        }
        return PrincipalContexts.current()
            .flatMap(context -> appendNow(eventType, schemaVersion, producerSubsystem, subjectType, subjectId, payloadJson, context))
            .switchIfEmpty(appendNow(eventType, schemaVersion, producerSubsystem, subjectType, subjectId, payloadJson, null))
            .as(transaction::transactional);
    }

    private Mono<OutboxEventEntity> appendNow(String eventType, int schemaVersion, String producerSubsystem,
                                              String subjectType, UUID subjectId, String payloadJson, PrincipalContext context) {
        return outbox.save(new OutboxEventEntity(null, eventType, schemaVersion, subjectType, subjectId,
            producerSubsystem, subjectType, subjectId,
            payloadJson, Instant.now(), 0, null, null,
            context == null ? null : context.requestId(),
            context == null ? null : context.correlationId(),
            context == null ? null : context.causationId(),
            context == null ? null : context.actorId()));
    }

    public Mono<Long> dispatchOnce(String consumerId, Function<OutboxEventEntity, Mono<Void>> handler) {
        if (consumerId == null || consumerId.isBlank()) {
            return Mono.error(new IllegalArgumentException("事件 Consumer ID 不合法"));
        }
        return outbox.findTop100PendingForConsumer(consumerId, Instant.now())
            .concatMap(event -> dispatchEvent(event, consumerId, handler, false))
            .reduce(0L, Long::sum);
    }

    public reactor.core.publisher.Flux<DurableEvent> pendingEvents() {
        return outbox.findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc().map(this::toDurableEvent);
    }

    public reactor.core.publisher.Flux<DurableEvent> pendingEvents(String consumerId) {
        if (consumerId == null || consumerId.isBlank()) return reactor.core.publisher.Flux.empty();
        return outbox.findTop100UndeliveredForConsumer(consumerId).map(this::toDurableEvent);
    }

    public Mono<Long> dispatchOnce(UUID eventId, DurableEventConsumer consumer) {
        if (consumer == null || consumer.consumerId() == null || consumer.consumerId().isBlank()) {
            return Mono.error(new IllegalArgumentException("事件 Consumer ID 不合法"));
        }
        return outbox.findById(eventId)
            .switchIfEmpty(Mono.error(new NotFoundException("事件不存在")))
            .flatMap(event -> dispatchEvent(event, consumer.consumerId(),
                candidate -> consumer.consume(toDurableEvent(candidate)), true));
    }

    private Mono<Long> dispatchEvent(OutboxEventEntity event, String consumerId,
                                     Function<OutboxEventEntity, Mono<Void>> handler, boolean forceRetry) {
        Instant now = Instant.now();
        return deliveries.insertIfAbsent(consumerId, event.id(), now)
            .then(transaction.transactional(deliveries.lockByConsumerIdAndEventId(consumerId, event.id())
                .switchIfEmpty(Mono.error(new NotFoundException("事件投递记录不存在")))
                .flatMap(delivery -> {
                    if ("DELIVERED".equals(delivery.status()) || (!forceRetry && "DEAD".equals(delivery.status()))
                        || (!forceRetry && delivery.nextAttemptAt().isAfter(now))) {
                        return Mono.just(0L);
                    }
                    return deliveries.recordAttempt(consumerId, event.id(), now)
                        .then(outbox.recordAttempt(event.id(), now))
                        .then(inbox.insertIfAbsent(consumerId, event.id(), now))
                        .flatMap(inserted -> inserted == 0
                            ? deliveries.markDelivered(consumerId, event.id(), Instant.now())
                                .then(mark(event)).thenReturn(1L)
                            : handler.apply(event)
                                .then(deliveries.markDelivered(consumerId, event.id(), Instant.now()))
                                .then(mark(event)).thenReturn(1L));
                })))
            .onErrorResume(error -> deliveries.recordFailure(consumerId, event.id(), Instant.now(),
                    error.getClass().getSimpleName(), MAX_DELIVERY_ATTEMPTS)
                .thenReturn(0L));
    }

    public Mono<Long> dispatchOnce(DurableEventConsumer consumer) {
        if (consumer == null || consumer.consumerId() == null || consumer.consumerId().isBlank()) {
            return Mono.error(new IllegalArgumentException("事件 Consumer ID 不合法"));
        }
        return dispatchOnce(consumer.consumerId(), event -> consumer.consume(toDurableEvent(event)));
    }

    private DurableEvent toDurableEvent(OutboxEventEntity event) {
        return new DurableEvent(event.id(), event.eventType(), event.schemaVersion(), event.producerSubsystem(),
            event.subjectType(), event.subjectId(), event.payloadJson(), event.occurredAt(), event.requestId(),
            event.correlationId(), event.causationId(), event.actorId());
    }

    private Mono<Void> mark(OutboxEventEntity event) {
        return outbox.markDispatched(event.id(), Instant.now()).then();
    }
}
