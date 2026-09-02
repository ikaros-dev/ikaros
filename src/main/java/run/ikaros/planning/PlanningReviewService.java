package run.ikaros.planning;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningReviewService {
    Mono<PlanningReviewView> create(UUID ownerId, CreatePlanningReviewRequest request);
    Flux<PlanningReviewView> list(UUID ownerId, PlanningReviewPeriod period);
    Mono<PlanningReviewView> update(UUID ownerId, UUID reviewId, UpdatePlanningReviewRequest request);
}
