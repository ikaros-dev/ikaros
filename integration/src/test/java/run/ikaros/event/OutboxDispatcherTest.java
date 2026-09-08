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
}
