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
