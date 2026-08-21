package run.ikaros.server.core.task;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import run.ikaros.api.constant.AppConst;
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

    protected abstract String getTaskEntityName();

    @Override
    public void run() {
        try {
            getRepository().save(getEntity().setStatus(TaskStatus.RUNNING))
                .block(AppConst.BLOCK_TIMEOUT);
            doRun();
            getRepository().save(getEntity()
                .setStatus(TaskStatus.FINISH)
                .setName(getTaskEntityName())
                .setEndTime(LocalDateTime.now())
            ).block(AppConst.BLOCK_TIMEOUT);
        } catch (Exception e) {
            log.error("exec task fail.", e);
            getRepository().save(
                getEntity()
                    .setFailMessage(e.getClass().getName() + ": " + e.getMessage())
                    .setStatus(TaskStatus.FAIL)
            ).block(AppConst.BLOCK_TIMEOUT);
        }
    }

    /**
     * 子类需要自己更新任务总数和任务进度.
     */
    protected abstract void doRun() throws Exception;
}
