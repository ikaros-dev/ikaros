package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningHabitCheckInRepository extends ReactiveCrudRepository<PlanningHabitCheckInEntity, UUID> {
    Flux<PlanningHabitCheckInEntity> findAllByOwnerIdAndHabitIdOrderByOccurredAtDesc(UUID ownerId, UUID habitId);
}
