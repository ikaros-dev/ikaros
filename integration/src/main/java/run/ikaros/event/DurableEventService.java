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
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.integration.api.EventReference;

/** Outbox 写入与 Inbox 幂等消费边界。 */
@Service
public class DurableEventService implements DurableEventPublisher {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final java.util.regex.Pattern EVENT_TYPE =
        java.util.regex.Pattern.compile("[a-z][a-z0-9-]*\\.[a-z][a-z0-9-]*\\.[a-z][a-z0-9-]*");
    private final OutboxEventRepository outbox;
    private final InboxEntryRepository inbox;
    private final TransactionalOperator transaction;

    public DurableEventService(OutboxEventRepository outbox, InboxEntryRepository inbox,
                               TransactionalOperator transaction) {
        this.outbox = outbox;
        this.inbox = inbox;
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
            .switchIfEmpty(appendNow(eventType, schemaVersion, producerSubsystem, subjectType, subjectId, payloadJson, null));
    }

    private Mono<OutboxEventEntity> appendNow(String eventType, int schemaVersion, String producerSubsystem,
                                              String subjectType, UUID subjectId, String payloadJson, PrincipalContext context) {
        return outbox.save(new OutboxEventEntity(null, eventType, schemaVersion, producerSubsystem, subjectType, subjectId,
            payloadJson, Instant.now(), 0, null, null,
            context == null ? null : context.requestId(),
            context == null ? null : context.correlationId(),
            context == null ? null : context.causationId(),
            context == null ? null : context.actorId()));
    }

    public Mono<Long> dispatchOnce(String consumerId, Function<OutboxEventEntity, Mono<Void>> handler) {
        return outbox.findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc()
            .concatMap(event -> inbox.existsByConsumerIdAndEventId(consumerId, event.id())
                .flatMap(processed -> processed
                    ? Mono.defer(() -> mark(event))
                    : outbox.recordAttempt(event.id(), Instant.now())
                        .then(transaction.transactional(Mono.defer(() -> inbox.save(new InboxEntryEntity(null, consumerId, event.id(), Instant.now())))
                            .then(handler.apply(event))
                            .then(Mono.defer(() -> mark(event)))))
                ).thenReturn(1L)
            )
            .reduce(0L, Long::sum);
    }

    private Mono<Void> mark(OutboxEventEntity event) {
        return outbox.markDispatched(event.id(), Instant.now()).then();
    }
}
