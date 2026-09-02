package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningHabitRepository extends ReactiveCrudRepository<PlanningHabitEntity, UUID> {
    Flux<PlanningHabitEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
