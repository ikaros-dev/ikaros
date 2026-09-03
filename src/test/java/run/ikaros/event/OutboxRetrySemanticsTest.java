package run.ikaros.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.mockito.InOrder;

class OutboxRetrySemanticsTest {
    @Test
    void failedHandlerDoesNotMarkEventDispatched() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        InboxEntryRepository inbox = mock(InboxEntryRepository.class);
        UUID id = UUID.randomUUID();
        OutboxEventEntity event = new OutboxEventEntity(id, "resource.resource.created", 1, "resource",
            id, "{}", Instant.now(), 0, null, null);
        when(outbox.findTop100ByDispatchedAtIsNullOrderByOccurredAtAsc()).thenReturn(Flux.just(event));
        when(inbox.existsByConsumerIdAndEventId("consumer", id)).thenReturn(Mono.just(false));
        when(outbox.recordAttempt(any(), any())).thenReturn(Mono.just(1));
        when(inbox.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DurableEventService service = new DurableEventService(outbox, inbox, transaction);
        Mono<Void> handler = Mono.error(new IllegalStateException());
        StepVerifier.create(service.dispatchOnce("consumer", ignored -> handler))
            .expectError(IllegalStateException.class).verify();
        InOrder order = inOrder(outbox);
        order.verify(outbox).recordAttempt(any(), any());
    }
}
