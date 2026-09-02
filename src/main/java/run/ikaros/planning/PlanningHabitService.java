package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningHabitService {
    Mono<PlanningHabitView> create(UUID ownerId, CreatePlanningHabitRequest request);
    Flux<PlanningHabitView> list(UUID ownerId);
    Mono<PlanningHabitView> archive(UUID ownerId, UUID habitId);
    Mono<PlanningHabitCheckInView> checkIn(UUID ownerId, UUID habitId, CreatePlanningHabitCheckInRequest request);
    Flux<PlanningHabitCheckInView> listCheckIns(UUID ownerId, UUID habitId);
}
