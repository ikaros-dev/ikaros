package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningMilestoneRepository extends ReactiveCrudRepository<PlanningMilestoneEntity, UUID> {
    Flux<PlanningMilestoneEntity> findAllByOwnerIdOrderByDueAtAsc(UUID ownerId);
    Flux<PlanningMilestoneEntity> findAllByOwnerIdAndGoalIdOrderByDueAtAsc(UUID ownerId, UUID goalId);
}
