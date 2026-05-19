package run.ikaros.server.core.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.TaskRepository;

class PluginTaskTest {

    @Test
    void getTaskEntityName() {
        TaskRepository repository = mock(TaskRepository.class);
        TaskEntity entity = TaskEntity.builder()
            .name("test-task")
            .status(TaskStatus.CREATE)
            .total(1L)
            .index(0L)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        PluginTask task = new PluginTask(entity, repository) {
            @Override
            protected String getTaskEntityName() {
                return "custom-task-name";
            }

            @Override
            protected void doRun() {
                // no-op
            }
        };

        assertThat(task.getTaskEntityName()).isEqualTo("custom-task-name");
        assertThat(task.getEntity()).isEqualTo(entity);
    }

    @Test
    void getEntity() {
        TaskRepository repository = mock(TaskRepository.class);
        TaskEntity entity = TaskEntity.builder()
            .name("test")
            .status(TaskStatus.CREATE)
            .total(0L)
            .index(0L)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        PluginTask task = new PluginTask(entity, repository) {
            @Override
            protected String getTaskEntityName() {
                return "test";
            }

            @Override
            protected void doRun() {
            }
        };

        assertThat(task.getEntity()).isNotNull();
        assertThat(task.getEntity().getName()).isEqualTo("test");
    }
}
