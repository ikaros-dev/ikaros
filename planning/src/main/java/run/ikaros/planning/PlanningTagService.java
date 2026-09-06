package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningTagService {
    Mono<PlanningTagView> create(UUID ownerId, CreatePlanningTagRequest request);
    Flux<PlanningTagView> list(UUID ownerId);
    Mono<Void> attach(UUID ownerId, UUID taskId, UUID tagId);
    Mono<Void> detach(UUID ownerId, UUID taskId, UUID tagId);
    Flux<PlanningTagView> listForTask(UUID ownerId, UUID taskId);
}
