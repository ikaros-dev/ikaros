package run.ikaros.operations.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.ikaros.operations.api.BackgroundTaskDispatcher;

class BackgroundTaskWorkerTest {
    @Test
    void doesNotExceedConfiguredInFlightTaskLimit() {
        BackgroundTaskDispatcher dispatcher = mock(BackgroundTaskDispatcher.class);
        when(dispatcher.dispatchOnce(anyString(), any())).thenReturn(Mono.never());
        BackgroundTaskWorker worker = new BackgroundTaskWorker(dispatcher, Duration.ofMinutes(1), 2);

        worker.dispatchOne();
        worker.dispatchOne();
        worker.dispatchOne();

        verify(dispatcher, times(2)).dispatchOnce(anyString(), eq(Duration.ofMinutes(1)));
    }
}
