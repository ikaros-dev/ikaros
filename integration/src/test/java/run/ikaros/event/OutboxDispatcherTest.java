package run.ikaros.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEventConsumer;

class OutboxDispatcherTest {
    @Test
    void rescansAllRegisteredConsumersWhenInvokedAfterRestart() {
        DurableEventService events = mock(DurableEventService.class);
        DurableEventConsumer consumer = mock(DurableEventConsumer.class);
        when(events.dispatchOnce(consumer)).thenReturn(Mono.just(1L));

        OutboxDispatcher dispatcher = new OutboxDispatcher(events, List.of(consumer));
        StepVerifier.create(dispatcher.dispatchNow()).verifyComplete();

        verify(events).dispatchOnce(consumer);
    }

    @Test
    void exposesPendingEventsAndRetriesByStableEventId() {
        DurableEventService events = mock(DurableEventService.class);
        DurableEventConsumer consumer = mock(DurableEventConsumer.class);
        java.util.UUID eventId = java.util.UUID.randomUUID();
        when(events.pendingEvents()).thenReturn(reactor.core.publisher.Flux.empty());
        when(events.dispatchOnce(eventId, consumer)).thenReturn(Mono.just(1L));

        OutboxDispatcher dispatcher = new OutboxDispatcher(events, List.of(consumer));
        StepVerifier.create(dispatcher.pendingEvents()).verifyComplete();
        StepVerifier.create(dispatcher.retry(eventId)).expectNext(1L).verifyComplete();

        verify(events).pendingEvents();
        verify(events).dispatchOnce(eventId, consumer);
    }
}
