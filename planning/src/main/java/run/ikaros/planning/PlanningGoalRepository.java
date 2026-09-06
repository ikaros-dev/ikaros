package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningGoalRepository extends ReactiveCrudRepository<PlanningGoalEntity, UUID> {
    Flux<PlanningGoalEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
