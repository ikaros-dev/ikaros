package run.ikaros.planning;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface PlanningTaskService {
    Mono<PlanningTaskView> create(UUID ownerId, CreatePlanningTaskRequest request);
    Flux<PlanningTaskView> list(UUID ownerId, PlanningTaskStatus status);
    Flux<PlanningTaskView> today(UUID ownerId, ZoneId zoneId);
    Flux<PlanningTaskView> upcoming(UUID ownerId, Instant from);
    Flux<PlanningTaskView> filter(UUID ownerId, PlanningTaskStatus status, PlanningTaskPriority priority,
        Instant from, Instant to, boolean overdue);
    Flux<PlanningTaskView> eisenhower(UUID ownerId, boolean important, boolean urgent);
    Mono<PlanningTaskView> update(UUID ownerId, UUID taskId, UpdatePlanningTaskRequest request);
    Mono<PlanningTaskView> changeStatus(UUID ownerId, UUID taskId, PlanningTaskStatus status);
}
