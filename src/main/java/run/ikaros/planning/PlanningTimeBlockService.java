package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningTimeBlockService {
    Mono<PlanningTimeBlockView> create(UUID ownerId, CreatePlanningTimeBlockRequest request);
    Flux<PlanningTimeBlockView> list(UUID ownerId, Instant from, Instant to);
    Mono<PlanningTimeBlockView> update(UUID ownerId, UUID blockId, UpdatePlanningTimeBlockRequest request);
    Mono<PlanningTimeBlockView> cancel(UUID ownerId, UUID blockId);
}
