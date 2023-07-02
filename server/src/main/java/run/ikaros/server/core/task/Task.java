package run.ikaros.server.core.task;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.TaskRepository;

@Data
@Slf4j
public abstract class Task implements Runnable {
    private final TaskEntity entity;
    private final TaskRepository repository;

    public Task(TaskEntity entity, TaskRepository repository) {
        this.entity = entity;
        this.repository = repository;
    }

    @Override
    public void run() {
        try {
            getRepository().save(getEntity().setStatus(TaskStatus.RUNNING)).block();
            doRun();
            getRepository().save(getEntity()
                .setStatus(TaskStatus.FINISH)
                .setEndTime(LocalDateTime.now())
            ).block();
        } catch (Exception e) {
            log.error("exec task fail.", e);
            getRepository().save(
                getEntity()
                    .setFailMessage(e.getClass().getName() + ": " + e.getMessage())
                    .setStatus(TaskStatus.FAIL)
            ).block();
        }
    }

    /**
     * 子类需要自己更新任务总数和任务进度.
     */
    protected abstract void doRun() throws Exception;
}
