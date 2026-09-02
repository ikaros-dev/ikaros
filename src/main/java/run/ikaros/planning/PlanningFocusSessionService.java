package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningFocusSessionService {
    Mono<PlanningFocusSessionView> start(UUID ownerId, StartPlanningFocusSessionRequest request);
    Flux<PlanningFocusSessionView> list(UUID ownerId);
    Mono<PlanningFocusSessionView> complete(UUID ownerId, UUID sessionId, CompletePlanningFocusSessionRequest request);
    Mono<PlanningFocusSessionView> cancel(UUID ownerId, UUID sessionId);
}
