package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface PlanningCalendarService {
    Flux<PlanningCalendarItemView> list(UUID ownerId, Instant from, Instant to);
}
