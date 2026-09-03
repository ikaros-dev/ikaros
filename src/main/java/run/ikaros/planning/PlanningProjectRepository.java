package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningProjectRepository extends ReactiveCrudRepository<PlanningProjectEntity, UUID> {
    Flux<PlanningProjectEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
