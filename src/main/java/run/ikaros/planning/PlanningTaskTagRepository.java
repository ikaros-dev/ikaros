package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningTaskTagRepository extends ReactiveCrudRepository<PlanningTaskTagEntity, UUID> {
    Flux<PlanningTaskTagEntity> findAllByTaskId(UUID taskId);
    Mono<PlanningTaskTagEntity> findByTaskIdAndTagId(UUID taskId, UUID tagId);
}
