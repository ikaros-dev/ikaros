package run.ikaros.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DurableEventDeliveryControllerTest {
    @Test
    void listsPendingEventsThroughDispatcher() {
        OutboxDispatcher dispatcher = mock(OutboxDispatcher.class);
        when(dispatcher.pendingEvents()).thenReturn(Flux.empty());

        DurableEventDeliveryController controller = new DurableEventDeliveryController(dispatcher);

        StepVerifier.create(controller.listPending()).verifyComplete();
        verify(dispatcher).pendingEvents();
    }

    @Test
    void returnsStableEventIdAndRetryCount() {
        OutboxDispatcher dispatcher = mock(OutboxDispatcher.class);
        UUID eventId = UUID.randomUUID();
        when(dispatcher.retry(eventId)).thenReturn(Mono.just(1L));

        DurableEventDeliveryController controller = new DurableEventDeliveryController(dispatcher);

        StepVerifier.create(controller.retry(eventId))
            .expectNext(new DurableEventRetryResult(eventId, 1L))
            .verifyComplete();
        verify(dispatcher).retry(eventId);
    }
}
