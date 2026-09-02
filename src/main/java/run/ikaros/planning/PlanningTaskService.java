package run.ikaros.planning;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface PlanningTaskService {
    Mono<PlanningTaskView> create(UUID ownerId, CreatePlanningTaskRequest request);
    Flux<PlanningTaskView> list(UUID ownerId, PlanningTaskStatus status);
    Mono<PlanningTaskView> update(UUID ownerId, UUID taskId, UpdatePlanningTaskRequest request);
    Mono<PlanningTaskView> changeStatus(UUID ownerId, UUID taskId, PlanningTaskStatus status);
}
