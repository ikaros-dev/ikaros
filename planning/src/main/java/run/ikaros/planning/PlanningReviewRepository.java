package run.ikaros.planning;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningReviewRepository extends ReactiveCrudRepository<PlanningReviewEntity, UUID> {
    Flux<PlanningReviewEntity> findAllByOwnerIdOrderByPeriodStartDesc(UUID ownerId);
    Mono<PlanningReviewEntity> findByOwnerIdAndPeriodAndPeriodStart(UUID ownerId, PlanningReviewPeriod period, java.time.Instant periodStart);
}
