package run.ikaros.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import static org.mockito.Mockito.mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.integration.api.DurableEventConsumer;

class DurableEventServiceTest {
    @Test
    void appendsOutboxFactInsideReactiveTransaction() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(outbox.save(any(OutboxEventEntity.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DurableEventService service = new DurableEventService(outbox, mock(InboxEntryRepository.class), transaction);
        StepVerifier.create(service.append(new EventAppendRequest("resource.resource.created", 1,
                "resource", "resource", UUID.randomUUID(), "{}")))
            .expectNextCount(1)
            .verifyComplete();

        verify(transaction).transactional(any(Mono.class));
        verify(outbox).save(any(OutboxEventEntity.class));
    }

    @Test
    void dispatchesThroughStableConsumerContract() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        InboxEntryRepository inbox = mock(InboxEntryRepository.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        OutboxEventEntity event = new OutboxEventEntity(UUID.randomUUID(), "resource.resource.created", 1,
            "resource", UUID.randomUUID(), "{}", java.time.Instant.now(), 0, null, null);
        when(outbox.findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc()).thenReturn(reactor.core.publisher.Flux.just(event));
        when(outbox.recordAttempt(any(), any())).thenReturn(Mono.just(1));
        when(outbox.markDispatched(any(), any())).thenReturn(Mono.just(1));
        when(inbox.insertIfAbsent(any(), any(), any())).thenReturn(Mono.just(1));
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DurableEventConsumer consumer = mock(DurableEventConsumer.class);
        when(consumer.consumerId()).thenReturn("consumer");
        when(consumer.consume(any())).thenReturn(Mono.empty());

        StepVerifier.create(new DurableEventService(outbox, inbox, transaction)
                .dispatchOnce(consumer))
            .expectNext(1L)
            .verifyComplete();
        verify(consumer).consume(any());
    }

    @Test
    void rejectsSecretLikePayloadsBeforePersistence() {
        DurableEventService service = new DurableEventService(mock(OutboxEventRepository.class),
            mock(InboxEntryRepository.class), mock(TransactionalOperator.class));
        assertThrows(RuntimeException.class, () -> service.append(new EventAppendRequest("resource.resource.created", 1,
            "resource", "resource", UUID.randomUUID(), "{\"access_token\":\"x\"}")).block());
    }

    @Test
    void rejectsMalformedOrNonObjectPayloadsBeforePersistence() {
        DurableEventService service = new DurableEventService(mock(OutboxEventRepository.class),
            mock(InboxEntryRepository.class), mock(TransactionalOperator.class));
        assertThrows(RuntimeException.class, () -> service.append(new EventAppendRequest("resource.resource.created", 1,
            "resource", "resource", UUID.randomUUID(), "not-json")).block());
        assertThrows(RuntimeException.class, () -> service.append(new EventAppendRequest("resource.resource.created", 1,
            "resource", "resource", UUID.randomUUID(), "[]")).block());
    }

    @Test
    void rejectsUnstableEventTypeNamesBeforePersistence() {
        DurableEventService service = new DurableEventService(mock(OutboxEventRepository.class),
            mock(InboxEntryRepository.class), mock(TransactionalOperator.class));
        assertThrows(RuntimeException.class, () -> service.append(new EventAppendRequest("ResourceCreated", 1,
            "resource", "resource", UUID.randomUUID(), "{}")).block());
    }
}
