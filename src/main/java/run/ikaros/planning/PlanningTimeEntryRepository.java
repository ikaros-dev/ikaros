package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningTimeEntryRepository extends ReactiveCrudRepository<PlanningTimeEntryEntity, UUID> {
    Flux<PlanningTimeEntryEntity> findAllByOwnerIdAndTaskIdOrderByCreatedAtDesc(UUID ownerId, UUID taskId);
}
