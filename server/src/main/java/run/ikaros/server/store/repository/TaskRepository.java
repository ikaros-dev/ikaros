package run.ikaros.server.store.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.store.entity.TaskEntity;

public interface TaskRepository extends R2dbcRepository<TaskEntity, Long> {
    Flux<TaskEntity> findAllByName(String name);

    Flux<TaskEntity> findAllByStatus(TaskStatus status);

    Flux<TaskEntity> findAllByNameLike(String name, Pageable pageable);

    Mono<Long> countAllByNameLike(String name);

    Flux<TaskEntity> findAllByNameLikeAndStatus(String name, TaskStatus status, Pageable pageable);

    Mono<Long> countAllByNameLikeAndStatus(String name, TaskStatus status);
}
