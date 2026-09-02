package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningTimeEntryService {
    Mono<PlanningTimeEntryView> create(UUID ownerId, UUID taskId, CreatePlanningTimeEntryRequest request);
    Flux<PlanningTimeEntryView> list(UUID ownerId, UUID taskId);
}
