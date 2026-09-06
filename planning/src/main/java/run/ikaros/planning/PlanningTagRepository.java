package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningTagRepository extends ReactiveCrudRepository<PlanningTagEntity, UUID> {
    Flux<PlanningTagEntity> findAllByOwnerIdOrderByName(UUID ownerId);
    Mono<PlanningTagEntity> findByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);
}
