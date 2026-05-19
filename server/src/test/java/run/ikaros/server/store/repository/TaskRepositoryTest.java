package run.ikaros.server.store.repository;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.TaskEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class TaskRepositoryTest {

    @Autowired
    TaskRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        TaskEntity entity = TaskEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name("test-task")
            .status(TaskStatus.CREATE)
            .total(10L)
            .index(0L)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findAllByName() {
        String name = "test-task-" + UuidV7Utils.generate().substring(0, 8);

        TaskEntity entity1 = TaskEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(name)
            .status(TaskStatus.CREATE)
            .build();

        TaskEntity entity2 = TaskEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(name)
            .status(TaskStatus.RUNNING)
            .build();

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllByName(name))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void findAllByStatus() {
        TaskEntity entity1 = TaskEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name("task-1")
            .status(TaskStatus.CREATE)
            .build();

        TaskEntity entity2 = TaskEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name("task-2")
            .status(TaskStatus.RUNNING)
            .build();

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllByStatus(TaskStatus.CREATE))
            .expectNextCount(1).verifyComplete();

        StepVerifier.create(repository.findAllByStatus(TaskStatus.RUNNING))
            .expectNextCount(1).verifyComplete();
    }

    @Test
    void findAllByNameLike() {
        String namePrefix = "searchable-task-" + UuidV7Utils.generate().substring(0, 8);

        for (int i = 0; i < 5; i++) {
            TaskEntity entity = TaskEntity.builder()
                .id(UuidV7Utils.generateUuid())
                .name(namePrefix + "-item-" + i)
                .status(TaskStatus.CREATE)
                .build();
            StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();
        }

        PageRequest pageRequest = PageRequest.of(0, 10);
        StepVerifier.create(repository.findAllByNameLike(namePrefix + "%", pageRequest))
            .expectNextCount(5).verifyComplete();
    }

    @Test
    void countAllByNameLike() {
        String namePrefix = "countable-task-" + UuidV7Utils.generate().substring(0, 8);

        for (int i = 0; i < 3; i++) {
            TaskEntity entity = TaskEntity.builder()
                .id(UuidV7Utils.generateUuid())
                .name(namePrefix + "-item-" + i)
                .status(TaskStatus.CREATE)
                .build();
            StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();
        }

        StepVerifier.create(repository.countAllByNameLike(namePrefix + "%"))
            .expectNext(3L).verifyComplete();
    }
}
