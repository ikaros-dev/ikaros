package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningGoalTaskRepository extends ReactiveCrudRepository<PlanningGoalTaskEntity, UUID> {
    Flux<PlanningGoalTaskEntity> findAllByGoalId(UUID goalId);
    Mono<PlanningGoalTaskEntity> findByGoalIdAndTaskId(UUID goalId, UUID taskId);
}
