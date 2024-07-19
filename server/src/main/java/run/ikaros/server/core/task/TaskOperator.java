package run.ikaros.server.core.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.task.TaskOperate;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.TaskRepository;

@Slf4j
@Component
public class TaskOperator implements TaskOperate {
    private final TaskService taskService;
    private final TaskRepository taskRepository;

    public TaskOperator(TaskService taskService,
                        TaskRepository taskRepository) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
    }

    @Override
    public Mono<Void> submit(String name, Runnable runnable) {
        Assert.hasText(name, "name must not be empty");
        Assert.notNull(runnable, "runnable must not be null");

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setName(name);
        taskService.setDefaultFieldValue(taskEntity);

        PluginTask pluginTask = new PluginTask(taskEntity, taskRepository) {
            @Override
            protected String getTaskEntityName() {
                return name;
            }

            @Override
            protected void doRun() throws Exception {
                runnable.run();
            }
        };

        return taskService.submit(pluginTask);
    }

    @Override
    public Mono<Void> cancel(String name) {
        Assert.hasText(name, "name must not be empty");
        return taskService.cancel(name);
    }

}
