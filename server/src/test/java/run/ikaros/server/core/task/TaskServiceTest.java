package run.ikaros.server.core.task;


import java.time.LocalDateTime;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.TaskRepository;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class TaskServiceTest {

    @Autowired
    TaskService taskService;
    @Autowired
    TaskRepository repository;

    class TestTask extends Task {
        Logger log = LoggerFactory.getLogger(TestTask.class);

        public TestTask(TaskEntity entity, TaskRepository repository) {
            super(entity, repository);
        }

        @Override
        protected String getTaskEntityName() {
            return getClass().getName() + '-';
        }

        @Override
        protected void doRun() throws Exception {
            log.info(getEntity().getName() + "-" + getEntity().getStatus());
        }
    }

    @Test
    void submit() {
        String name = "UnitTestTask-" + new Random().nextInt(10000);
        LocalDateTime now = LocalDateTime.now();
        TestTask task = new TestTask(TaskEntity.builder()
            .name(name)
            .createTime(now)
            .startTime(now)
            .status(TaskStatus.CREATE)
            .total(1L)
            .index(0L)
            .build(), repository);

        StepVerifier.create(taskService.submit(task)).verifyComplete();
    }

    @Test
    void findById() {
        String name = "UnitTestTask-" + new Random().nextInt(10000);
        LocalDateTime now = LocalDateTime.now();
        TaskEntity entity = TaskEntity.builder()
            .id(run.ikaros.api.infra.utils.UuidV7Utils.generateUuid())
            .name(name)
            .createTime(now)
            .startTime(now)
            .status(TaskStatus.CREATE)
            .total(1L)
            .index(0L)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNextCount(1).verifyComplete();

        StepVerifier.create(taskService.findById(entity.getId()))
            .expectNextMatches(taskEntity -> name.equals(taskEntity.getName()))
            .verifyComplete();
    }

    @Test
    void listEntitiesByCondition() {
        String name = "UnitTestTask-" + new Random().nextInt(10000);
        LocalDateTime now = LocalDateTime.now();
        TaskEntity entity = TaskEntity.builder()
            .id(run.ikaros.api.infra.utils.UuidV7Utils.generateUuid())
            .name(name)
            .createTime(now)
            .startTime(now)
            .status(TaskStatus.CREATE)
            .total(1L)
            .index(0L)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNextCount(1).verifyComplete();

        FindTaskCondition condition = FindTaskCondition.builder()
            .page(1)
            .size(10)
            .name(name)
            .build();

        StepVerifier.create(taskService.listEntitiesByCondition(condition))
            .expectNextMatches(pagingWrap ->
                pagingWrap.getPage() == 1
                    && pagingWrap.getSize() == 10
                    && pagingWrap.getTotal() >= 1)
            .verifyComplete();
    }
}