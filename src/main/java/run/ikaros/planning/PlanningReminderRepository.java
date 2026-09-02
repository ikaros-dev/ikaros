package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningReminderRepository extends ReactiveCrudRepository<PlanningReminderEntity, UUID> {
    Flux<PlanningReminderEntity> findAllByOwnerIdOrderByTriggerAt(UUID ownerId);
    Flux<PlanningReminderEntity> findAllByStatusAndTriggerAtLessThanEqual(PlanningReminderStatus status,
        java.time.Instant triggerAt);
}
