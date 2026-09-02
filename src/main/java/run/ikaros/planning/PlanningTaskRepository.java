package run.ikaros.planning;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
public interface PlanningTaskRepository extends ReactiveCrudRepository<PlanningTaskEntity, UUID> {
    Flux<PlanningTaskEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Flux<PlanningTaskEntity> findAllByOwnerIdAndStatusOrderByCreatedAtDesc(UUID ownerId, PlanningTaskStatus status);
    Flux<PlanningTaskEntity> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
