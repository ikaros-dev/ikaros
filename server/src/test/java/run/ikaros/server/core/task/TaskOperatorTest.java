package run.ikaros.server.core.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.TaskRepository;

class TaskOperatorTest {

    private TaskService taskService;
    private TaskRepository taskRepository;
    private TaskOperator taskOperator;

    @BeforeEach
    void setUp() {
        taskService = mock(TaskService.class);
        taskRepository = mock(TaskRepository.class);
        taskOperator = new TaskOperator(taskService, taskRepository);
    }

    @Test
    void constructorCreatesInstance() {
        assertThat(taskOperator).isNotNull();
    }

    @Test
    void submit_withValidParams_submitsTask() {
        String taskName = "test-task";
        Runnable runnable = () -> System.out.println("test");
        
        when(taskService.submit(any(PluginTask.class))).thenReturn(Mono.empty());

        StepVerifier.create(taskOperator.submit(taskName, runnable))
            .verifyComplete();

        verify(taskService).submit(any(PluginTask.class));
    }

    @Test
    void submit_withEmptyName_throwsException() {
        Runnable runnable = () -> System.out.println("test");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> taskOperator.submit("", runnable).block())
            .withMessageContaining("name must not be empty");
    }

    @Test
    void submit_withNullRunnable_throwsException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> taskOperator.submit("test", null).block())
            .withMessageContaining("runnable must not be null");
    }

    @Test
    void submit_withDelay_submitsTaskWithDelay() {
        String taskName = "delayed-task";
        Runnable runnable = () -> System.out.println("delayed test");
        Duration delay = Duration.ofMinutes(5);

        when(taskService.submit(any(PluginTask.class))).thenReturn(Mono.empty());

        StepVerifier.create(taskOperator.submit(taskName, runnable, delay))
            .verifyComplete();

        verify(taskService).submit(any(PluginTask.class));
    }

    @Test
    void submit_withDelayAndEmptyName_throwsException() {
        Runnable runnable = () -> System.out.println("test");
        Duration delay = Duration.ofMinutes(5);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> taskOperator.submit("", runnable, delay).block())
            .withMessageContaining("name must not be empty");
    }

    @Test
    void submit_withDelayAndNullRunnable_throwsException() {
        Duration delay = Duration.ofMinutes(5);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> taskOperator.submit("test", null, delay).block())
            .withMessageContaining("runnable must not be null");
    }

    @Test
    void cancel_withValidName_cancelsTask() {
        String taskName = "test-task";
        
        when(taskService.cancel(taskName)).thenReturn(Mono.empty());

        StepVerifier.create(taskOperator.cancel(taskName))
            .verifyComplete();

        verify(taskService).cancel(taskName);
    }

    @Test
    void cancel_withEmptyName_throwsException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> taskOperator.cancel("").block())
            .withMessageContaining("name must not be empty");
    }

    @Test
    void cancel_withNullName_throwsException() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> taskOperator.cancel(null).block())
            .withMessageContaining("name must not be empty");
    }
}
