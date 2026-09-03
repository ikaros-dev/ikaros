package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface PlanningStatisticsService {
    Mono<PlanningStatisticsView> summarize(UUID ownerId, Instant from, Instant to);
}
