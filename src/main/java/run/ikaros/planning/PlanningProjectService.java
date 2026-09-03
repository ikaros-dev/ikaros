package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningProjectService {
    Mono<PlanningProjectView> create(UUID ownerId, CreatePlanningProjectRequest request);
    Flux<PlanningProjectView> list(UUID ownerId);
    Mono<PlanningProjectView> update(UUID ownerId, UUID projectId, UpdatePlanningProjectRequest request);
    Mono<PlanningProjectView> changeStatus(UUID ownerId, UUID projectId, PlanningProjectStatus status);
    Mono<PlanningProjectView> changeStatus(UUID ownerId, UUID projectId, PlanningProjectStatus status,
                                            long expectedVersion);
}
