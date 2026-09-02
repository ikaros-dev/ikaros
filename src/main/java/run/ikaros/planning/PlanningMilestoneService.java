package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningMilestoneService {
    Mono<PlanningMilestoneView> create(UUID ownerId, CreatePlanningMilestoneRequest request);
    Flux<PlanningMilestoneView> list(UUID ownerId, UUID goalId);
    Mono<PlanningMilestoneView> update(UUID ownerId, UUID milestoneId, UpdatePlanningMilestoneRequest request);
    Mono<PlanningMilestoneView> changeStatus(UUID ownerId, UUID milestoneId, PlanningMilestoneStatus status);
}
