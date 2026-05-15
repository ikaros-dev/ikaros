package run.ikaros.server.core.task;


import java.time.LocalDateTime;
import java.util.Random;
import org.junit.jupiter.api.Disabled;
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

@Disabled
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
            //System.out.println(getEntity().getName() + "-" + getEntity().getStatus());
            log.info(getEntity().getName() + "-" + getEntity().getStatus());
        }
    }

    @Test
    void submit() throws InterruptedException {
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

        taskService.updateTaskStatus();
        Thread.sleep(500);

    }
}