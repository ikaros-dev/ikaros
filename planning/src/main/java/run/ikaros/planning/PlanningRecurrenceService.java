package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface PlanningRecurrenceService {
    Mono<PlanningRecurrenceView> create(UUID ownerId, UUID taskId, CreatePlanningRecurrenceRequest request);
    Mono<PlanningRecurrenceView> get(UUID ownerId, UUID taskId);
    Mono<PlanningRecurrenceView> setActive(UUID ownerId, UUID taskId, boolean active);
    Mono<PlanningRecurrenceView> skip(UUID ownerId, UUID taskId, java.time.Instant nextRunAt);
}
