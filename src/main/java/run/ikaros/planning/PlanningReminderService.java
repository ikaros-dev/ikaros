package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanningReminderService {
    Mono<PlanningReminderView> create(UUID ownerId, CreatePlanningReminderRequest request);
    Flux<PlanningReminderView> list(UUID ownerId);
    Mono<PlanningReminderView> acknowledge(UUID ownerId, UUID reminderId);
    Mono<PlanningReminderView> acknowledge(UUID ownerId, UUID reminderId, long expectedVersion);
    Mono<PlanningReminderView> snooze(UUID ownerId, UUID reminderId, Instant until);
    Mono<PlanningReminderView> snooze(UUID ownerId, UUID reminderId, Instant until, long expectedVersion);
    Mono<PlanningReminderView> cancel(UUID ownerId, UUID reminderId);
    Mono<PlanningReminderView> cancel(UUID ownerId, UUID reminderId, long expectedVersion);
}
