package run.ikaros.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.mockito.InOrder;

class OutboxRetrySemanticsTest {
    @Test
    void concurrentRepeatedDispatchRunsHandlerOnlyOnce() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        InboxEntryRepository inbox = mock(InboxEntryRepository.class);
        UUID id = UUID.randomUUID();
        OutboxEventEntity event = new OutboxEventEntity(id, "resource.resource.created", 1, "resource",
            id, "{}", Instant.now(), 0, null, null);
        when(outbox.findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc()).thenReturn(Flux.just(event));
        AtomicBoolean claimed = new AtomicBoolean();
        when(inbox.insertIfAbsent(any(), any(), any()))
            .thenAnswer(invocation -> Mono.fromSupplier(() -> claimed.compareAndSet(false, true) ? 1 : 0));
        when(outbox.recordAttempt(any(), any())).thenReturn(Mono.just(1));
        when(outbox.markDispatched(any(), any())).thenReturn(Mono.just(1));
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DurableEventService service = new DurableEventService(outbox, inbox, transaction);
        AtomicInteger handlerCalls = new AtomicInteger();

        StepVerifier.create(Flux.merge(
                service.dispatchOnce("consumer", ignored -> {
                    handlerCalls.incrementAndGet();
                    return Mono.empty();
                }),
                service.dispatchOnce("consumer", ignored -> {
                    handlerCalls.incrementAndGet();
                    return Mono.empty();
                })))
            .expectNextCount(2).verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(1, handlerCalls.get());
    }

    @Test
    void failedHandlerDoesNotMarkEventDispatched() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        InboxEntryRepository inbox = mock(InboxEntryRepository.class);
        UUID id = UUID.randomUUID();
        OutboxEventEntity event = new OutboxEventEntity(id, "resource.resource.created", 1, "resource",
            id, "{}", Instant.now(), 0, null, null);
        when(outbox.findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc()).thenReturn(Flux.just(event));
        when(inbox.insertIfAbsent(any(), any(), any())).thenReturn(Mono.just(1));
        when(outbox.recordAttempt(any(), any())).thenReturn(Mono.just(1));
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DurableEventService service = new DurableEventService(outbox, inbox, transaction);
        Mono<Void> handler = Mono.error(new IllegalStateException());
        StepVerifier.create(service.dispatchOnce("consumer", ignored -> handler))
            .expectError(IllegalStateException.class).verify();
        InOrder order = inOrder(outbox);
        order.verify(outbox).recordAttempt(any(), any());
    }

    @Test
    void alreadyProcessedEventSkipsHandler() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        InboxEntryRepository inbox = mock(InboxEntryRepository.class);
        UUID id = UUID.randomUUID();
        OutboxEventEntity event = new OutboxEventEntity(id, "resource.resource.created", 1, "resource",
            id, "{}", Instant.now(), 1, Instant.now(), null);
        when(outbox.findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc()).thenReturn(Flux.just(event));
        when(inbox.insertIfAbsent(any(), any(), any())).thenReturn(Mono.just(0));
        when(outbox.recordAttempt(any(), any())).thenReturn(Mono.just(1));
        when(outbox.markDispatched(any(), any())).thenReturn(Mono.just(1));
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DurableEventService service = new DurableEventService(outbox, inbox, transaction);
        AtomicBoolean called = new AtomicBoolean();

        StepVerifier.create(service.dispatchOnce("consumer", ignored -> {
            called.set(true);
            return Mono.empty();
        })).expectNext(1L).verifyComplete();

        org.junit.jupiter.api.Assertions.assertFalse(called.get());
    }
}
