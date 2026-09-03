package run.ikaros.event;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.security.PrincipalContexts;
import run.ikaros.security.PrincipalContext;

/** Outbox 写入与 Inbox 幂等消费边界。 */
@Service
public class DurableEventService {
    private final OutboxEventRepository outbox;
    private final InboxEntryRepository inbox;
    private final TransactionalOperator transaction;

    public DurableEventService(OutboxEventRepository outbox, InboxEntryRepository inbox,
                               TransactionalOperator transaction) {
        this.outbox = outbox;
        this.inbox = inbox;
        this.transaction = transaction;
    }

    public Mono<OutboxEventEntity> append(String eventType, int schemaVersion, String aggregateType,
                                         UUID aggregateId, String payloadJson) {
        if (eventType == null || eventType.isBlank() || schemaVersion < 1 || payloadJson == null) {
            return Mono.error(new IllegalArgumentException("事件类型、版本和 Payload 不合法"));
        }
        String normalized = payloadJson.toLowerCase();
        if (normalized.contains("password") || normalized.contains("secret")
            || normalized.contains("private_key") || normalized.contains("access_token")
            || normalized.contains("refresh_token")) {
            return Mono.error(new IllegalArgumentException("事件 Payload 不得包含 Secret 或 Token"));
        }
        return PrincipalContexts.current()
            .flatMap(context -> appendNow(eventType, schemaVersion, aggregateType, aggregateId, payloadJson, context))
            .switchIfEmpty(appendNow(eventType, schemaVersion, aggregateType, aggregateId, payloadJson, null));
    }

    private Mono<OutboxEventEntity> appendNow(String eventType, int schemaVersion, String aggregateType,
                                              UUID aggregateId, String payloadJson, PrincipalContext context) {
        return outbox.save(new OutboxEventEntity(null, eventType, schemaVersion, aggregateType, aggregateId,
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
                        .then(transaction.transactional(handler.apply(event)
                            .then(Mono.defer(() -> inbox.save(new InboxEntryEntity(null, consumerId, event.id(), Instant.now()))))
                            .then(Mono.defer(() -> mark(event)))))
                )
            )
            .count();
    }

    private Mono<Void> mark(OutboxEventEntity event) {
        return outbox.markDispatched(event.id(), Instant.now()).then();
    }
}
