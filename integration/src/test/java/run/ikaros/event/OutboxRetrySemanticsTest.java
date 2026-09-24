package run.ikaros.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OutboxRetrySemanticsTest {
    @Test
    void concurrentRepeatedDispatchRunsHandlerOnlyOnce() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        InboxEntryRepository inbox = mock(InboxEntryRepository.class);
        OutboxDeliveryRepository deliveries = mock(OutboxDeliveryRepository.class);
        UUID id = UUID.randomUUID();
        OutboxEventEntity event = event(id);
        when(outbox.findTop100PendingForConsumer(any(), any())).thenReturn(Flux.just(event));
        when(deliveries.insertIfAbsent(any(), any(), any())).thenReturn(Mono.just(1));
        when(deliveries.lockByConsumerIdAndEventId(any(), any())).thenReturn(Mono.just(delivery("consumer", id, "PENDING")));
        AtomicBoolean inboxClaimed = new AtomicBoolean();
        when(inbox.insertIfAbsent(any(), any(), any()))
            .thenAnswer(invocation -> Mono.fromSupplier(() -> inboxClaimed.compareAndSet(false, true) ? 1 : 0));
        when(deliveries.recordAttempt(any(), any(), any())).thenReturn(Mono.just(1));
        when(deliveries.markDelivered(any(), any(), any())).thenReturn(Mono.just(1));
        when(outbox.recordAttempt(any(), any())).thenReturn(Mono.just(1));
        when(outbox.markDispatched(any(), any())).thenReturn(Mono.just(1));
        TransactionalOperator transaction = transaction();
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DurableEventService service = new DurableEventService(outbox, inbox, deliveries, transaction);
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
    void failedHandlerIsRecordedForOnlyItsConsumerAndAnotherConsumerCanSucceed() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        InboxEntryRepository inbox = mock(InboxEntryRepository.class);
        OutboxDeliveryRepository deliveries = mock(OutboxDeliveryRepository.class);
        UUID id = UUID.randomUUID();
        OutboxEventEntity event = event(id);
        when(outbox.findTop100PendingForConsumer(any(), any())).thenReturn(Flux.just(event));
        when(deliveries.insertIfAbsent(any(), any(), any())).thenReturn(Mono.just(1));
        when(deliveries.lockByConsumerIdAndEventId(any(), any())).thenAnswer(invocation ->
            Mono.just(delivery(invocation.getArgument(0), id, "PENDING")));
        when(inbox.insertIfAbsent(any(), any(), any())).thenReturn(Mono.just(1));
        when(deliveries.recordAttempt(any(), any(), any())).thenReturn(Mono.just(1));
        when(deliveries.markDelivered(any(), any(), any())).thenReturn(Mono.just(1));
        when(deliveries.recordFailure(any(), any(), any(), any(), anyInt())).thenReturn(Mono.just(1));
        when(outbox.recordAttempt(any(), any())).thenReturn(Mono.just(1));
        when(outbox.markDispatched(any(), any())).thenReturn(Mono.just(1));
        TransactionalOperator transaction = transaction();
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DurableEventService service = new DurableEventService(outbox, inbox, deliveries, transaction);
        AtomicInteger searchCalls = new AtomicInteger();

        StepVerifier.create(service.dispatchOnce("notification", ignored -> Mono.error(new IllegalStateException("offline"))))
            .expectNext(0L).verifyComplete();
        StepVerifier.create(service.dispatchOnce("search", ignored -> {
            searchCalls.incrementAndGet();
            return Mono.empty();
        })).expectNext(1L).verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(1, searchCalls.get());
        verify(deliveries).recordFailure(org.mockito.ArgumentMatchers.eq("notification"), org.mockito.ArgumentMatchers.eq(id),
            any(), org.mockito.ArgumentMatchers.eq("IllegalStateException"), anyInt());
    }

    @Test
    void deliveredEventDoesNotRunHandlerAgain() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        InboxEntryRepository inbox = mock(InboxEntryRepository.class);
        OutboxDeliveryRepository deliveries = mock(OutboxDeliveryRepository.class);
        UUID id = UUID.randomUUID();
        when(outbox.findTop100PendingForConsumer(any(), any())).thenReturn(Flux.just(event(id)));
        when(deliveries.insertIfAbsent(any(), any(), any())).thenReturn(Mono.just(0));
        when(deliveries.lockByConsumerIdAndEventId(any(), any()))
            .thenReturn(Mono.just(delivery("consumer", id, "DELIVERED")));
        TransactionalOperator transaction = transaction();
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DurableEventService service = new DurableEventService(outbox, inbox, deliveries, transaction);

        StepVerifier.create(service.dispatchOnce("consumer", ignored -> Mono.error(new AssertionError("must not run"))))
            .expectNext(0L).verifyComplete();
    }

    private static TransactionalOperator transaction() {
        return mock(TransactionalOperator.class);
    }

    private static OutboxEventEntity event(UUID id) {
        return new OutboxEventEntity(id, "resource.resource.created", 1, "resource", id, "{}", Instant.now(), 0, null, null);
    }

    private static OutboxDeliveryEntity delivery(String consumer, UUID eventId, String status) {
        Instant now = Instant.now();
        return new OutboxDeliveryEntity(UUID.randomUUID(), consumer, eventId, status, 0, now.minusSeconds(1),
            null, null, now, now);
    }
}
