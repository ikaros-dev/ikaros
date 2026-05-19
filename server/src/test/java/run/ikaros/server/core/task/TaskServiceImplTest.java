package run.ikaros.server.core.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.TaskRepository;

class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ExecutorService executorService;
    private TaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        taskService = new TaskServiceImpl(taskRepository, executorService);
    }

    @Test
    void findById_found() {
        UUID id = UuidV7Utils.generateUuid();
        TaskEntity entity = TaskEntity.builder()
            .name("test-task")
            .status(TaskStatus.CREATE)
            .total(1L)
            .index(0L)
            .build();
        entity.setId(id);

        when(taskRepository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier.create(taskService.findById(id))
            .assertNext(e -> assertThat(e.getName()).isEqualTo("test-task"))
            .verifyComplete();
    }

    @Test
    void findById_notFound() {
        UUID id = UuidV7Utils.generateUuid();
        when(taskRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(taskService.findById(id))
            .verifyComplete();
    }

    @Test
    void listEntitiesByCondition_withStatus() {
        TaskEntity entity = TaskEntity.builder()
            .name("test-task")
            .status(TaskStatus.RUNNING)
            .createTime(LocalDateTime.now())
            .total(1L)
            .index(0L)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        when(taskRepository.findAllByNameLikeAndStatus(any(), any(), any()))
            .thenReturn(Flux.just(entity));
        when(taskRepository.countAllByNameLikeAndStatus(any(), any()))
            .thenReturn(Mono.just(1L));

        FindTaskCondition condition = FindTaskCondition.builder()
            .page(1)
            .size(10)
            .name("test")
            .status(TaskStatus.RUNNING)
            .build();

        StepVerifier.create(taskService.listEntitiesByCondition(condition))
            .assertNext(paging -> {
                assertThat(paging.getPage()).isEqualTo(1);
                assertThat(paging.getSize()).isEqualTo(10);
                assertThat(paging.getTotal()).isEqualTo(1L);
            })
            .verifyComplete();
    }

    @Test
    void listEntitiesByCondition_withoutStatus() {
        TaskEntity entity = TaskEntity.builder()
            .name("test-task")
            .status(TaskStatus.CREATE)
            .createTime(LocalDateTime.now())
            .total(1L)
            .index(0L)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        when(taskRepository.findAllByNameLike(any(), any()))
            .thenReturn(Flux.just(entity));
        when(taskRepository.countAllByNameLike(any()))
            .thenReturn(Mono.just(1L));

        FindTaskCondition condition = FindTaskCondition.builder()
            .page(1)
            .size(10)
            .build();

        StepVerifier.create(taskService.listEntitiesByCondition(condition))
            .assertNext(paging -> {
                assertThat(paging.getPage()).isEqualTo(1);
                assertThat(paging.getSize()).isEqualTo(10);
                assertThat(paging.getTotal()).isEqualTo(1L);
            })
            .verifyComplete();
    }

    @Test
    void setDefaultFieldValue_setsDefaults() {
        TaskEntity entity = new TaskEntity();
        taskService.setDefaultFieldValue(entity);

        assertThat(entity.getStatus()).isEqualTo(TaskStatus.CREATE);
        assertThat(entity.getTotal()).isEqualTo(0L);
        assertThat(entity.getIndex()).isEqualTo(0L);
        assertThat(entity.getCreateTime()).isNotNull();
    }

    @Test
    void setDefaultFieldValue_doesNotOverrideExisting() {
        TaskEntity entity = TaskEntity.builder()
            .status(TaskStatus.RUNNING)
            .total(10L)
            .index(5L)
            .createTime(LocalDateTime.of(2024, 1, 1, 0, 0))
            .build();

        taskService.setDefaultFieldValue(entity);

        assertThat(entity.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(entity.getTotal()).isEqualTo(10L);
        assertThat(entity.getIndex()).isEqualTo(5L);
    }

    @Test
    void getProcess_withTotal() {
        UUID id = UuidV7Utils.generateUuid();
        TaskEntity entity = TaskEntity.builder()
            .name("test")
            .status(TaskStatus.RUNNING)
            .total(100L)
            .index(50L)
            .build();
        entity.setId(id);

        when(taskRepository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier.create(taskService.getProcess(id))
            .assertNext(process -> assertThat(process).isEqualTo(50L))
            .verifyComplete();
    }

    @Test
    void getProcess_zeroTotal() {
        UUID id = UuidV7Utils.generateUuid();
        TaskEntity entity = TaskEntity.builder()
            .name("test")
            .status(TaskStatus.CREATE)
            .total(0L)
            .index(0L)
            .build();
        entity.setId(id);

        when(taskRepository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier.create(taskService.getProcess(id))
            .assertNext(process -> assertThat(process).isEqualTo(0L))
            .verifyComplete();
    }
}
