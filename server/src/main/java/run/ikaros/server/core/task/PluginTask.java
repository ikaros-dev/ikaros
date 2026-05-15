package run.ikaros.server.core.task;

import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.TaskRepository;

public class PluginTask extends Task {
    public PluginTask(TaskEntity entity,
                      TaskRepository repository) {
        super(entity, repository);
    }

    @Override
    protected String getTaskEntityName() {
        return this.getClass().getName() + "-";
    }

    @Override
    protected void doRun() throws Exception {

    }
}
