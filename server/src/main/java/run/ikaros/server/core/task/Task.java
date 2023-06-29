package run.ikaros.server.core.task;

import lombok.Data;
import run.ikaros.server.store.entity.TaskEntity;

@Data
public abstract class Task implements Runnable {
    private final TaskEntity entity;

    public Task(TaskEntity entity) {
        this.entity = entity;
    }

}
