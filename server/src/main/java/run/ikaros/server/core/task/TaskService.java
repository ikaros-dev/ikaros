package run.ikaros.server.core.task;

import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.TaskEntity;

public interface TaskService {
    void updateTaskStatus();

    Mono<TaskEntity> findById(Long id);

    Mono<TaskEntity> findByName(String name);

    Mono<Void> submit(Task task);

    Mono<Void> cancel(String name);
}
