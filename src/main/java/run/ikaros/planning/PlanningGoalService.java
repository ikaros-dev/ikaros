package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningGoalService {
    Mono<PlanningGoalView> create(UUID ownerId, CreatePlanningGoalRequest request);
    Flux<PlanningGoalView> list(UUID ownerId);
    Mono<PlanningGoalView> update(UUID ownerId, UUID goalId, UpdatePlanningGoalRequest request);
    Mono<PlanningGoalView> updateProgress(UUID ownerId, UUID goalId, UpdatePlanningGoalProgressRequest request);
    Mono<PlanningGoalView> changeStatus(UUID ownerId, UUID goalId, PlanningGoalStatus status);
    Mono<Void> attachTask(UUID ownerId, UUID goalId, UUID taskId);
    Mono<Void> detachTask(UUID ownerId, UUID goalId, UUID taskId);
}
