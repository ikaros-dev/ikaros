package run.ikaros.server.core.task;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.TaskEntity;

public interface TaskService {
    void updateTaskStatus();

    void updateAllRunningAndCreatedTaskStatusToCancel();

    Mono<TaskEntity> findById(UUID id);

    Mono<Void> submit(Task task);

    Mono<Void> cancel(String name);

    Mono<PagingWrap<TaskEntity>> listEntitiesByCondition(FindTaskCondition findTaskCondition);

    Mono<Long> getProcess(UUID id);

    void setDefaultFieldValue(TaskEntity entity);
}
