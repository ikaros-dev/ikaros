package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningTaskDependencyRepository extends ReactiveCrudRepository<PlanningTaskDependencyEntity, UUID> {
    Flux<PlanningTaskDependencyEntity> findAllByTaskId(UUID taskId);
    Mono<PlanningTaskDependencyEntity> findByTaskIdAndDependsOnTaskId(UUID taskId, UUID dependsOnTaskId);
}
