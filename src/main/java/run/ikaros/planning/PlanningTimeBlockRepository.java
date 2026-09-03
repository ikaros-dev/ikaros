package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlanningTimeBlockRepository extends ReactiveCrudRepository<PlanningTimeBlockEntity, UUID> {
    Flux<PlanningTimeBlockEntity> findAllByOwnerIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAt(
        UUID ownerId, Instant endAt, Instant startAt);
    Flux<PlanningTimeBlockEntity> findAllByOwnerIdOrderByStartAt(UUID ownerId);
}
