package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PlanningRecurrenceRepository extends ReactiveCrudRepository<PlanningRecurrenceEntity, UUID> {
    Mono<PlanningRecurrenceEntity> findByOwnerIdAndTaskId(UUID ownerId, UUID taskId);
}
