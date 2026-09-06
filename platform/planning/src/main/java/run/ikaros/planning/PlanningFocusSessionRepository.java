package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningFocusSessionRepository extends ReactiveCrudRepository<PlanningFocusSessionEntity, UUID> {
    Flux<PlanningFocusSessionEntity> findAllByOwnerIdOrderByStartedAtDesc(UUID ownerId);
}
