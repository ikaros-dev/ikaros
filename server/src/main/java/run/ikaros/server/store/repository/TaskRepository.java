package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.TaskEntity;

public interface TaskRepository extends R2dbcRepository<TaskEntity, Long> {
    Mono<TaskEntity> findByName(String name);
}
