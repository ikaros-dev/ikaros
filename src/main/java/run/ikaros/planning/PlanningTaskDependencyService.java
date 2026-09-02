package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningTaskDependencyService {
    Mono<PlanningTaskDependencyView> create(UUID ownerId, UUID taskId, CreatePlanningTaskDependencyRequest request);
    Flux<PlanningTaskDependencyView> list(UUID ownerId, UUID taskId);
    Mono<Void> delete(UUID ownerId, UUID taskId, UUID dependsOnTaskId);
}
