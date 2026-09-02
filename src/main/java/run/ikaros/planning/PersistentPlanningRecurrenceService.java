package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningRecurrenceService implements PlanningRecurrenceService {
    private final PlanningRecurrenceRepository recurrences;
    private final PlanningTaskRepository tasks;

    public PersistentPlanningRecurrenceService(PlanningRecurrenceRepository recurrences, PlanningTaskRepository tasks) {
        this.recurrences = recurrences; this.tasks = tasks;
    }

    @Override public Mono<PlanningRecurrenceView> create(UUID ownerId, UUID taskId, CreatePlanningRecurrenceRequest request) {
        return ownedTask(ownerId, taskId).then(recurrences.findByOwnerIdAndTaskId(ownerId, taskId)
            .flatMap(existing -> Mono.<PlanningRecurrenceView>error(new ConflictException("Task 已存在重复规则")))
            .switchIfEmpty(recurrences.save(new PlanningRecurrenceEntity(null, ownerId, taskId, request.rule().trim(),
                request.mode() == null ? PlanningRecurrenceMode.FIXED_SCHEDULE : request.mode(),
                request.timeZone() == null ? "UTC" : request.timeZone(), request.nextRunAt(), true, null,
                Instant.now(), Instant.now(), null)).map(this::view)));
    }

    @Override public Mono<PlanningRecurrenceView> get(UUID ownerId, UUID taskId) {
        return owned(ownerId, taskId).map(this::view);
    }

    @Override public Mono<PlanningRecurrenceView> setActive(UUID ownerId, UUID taskId, boolean active) {
        return owned(ownerId, taskId).flatMap(old -> save(old, old.nextRunAt(), active));
    }

    @Override public Mono<PlanningRecurrenceView> skip(UUID ownerId, UUID taskId, Instant nextRunAt) {
        return owned(ownerId, taskId).flatMap(old -> save(old, nextRunAt, old.active()));
    }

    private Mono<PlanningRecurrenceEntity> owned(UUID ownerId, UUID taskId) {
        return recurrences.findByOwnerIdAndTaskId(ownerId, taskId)
            .switchIfEmpty(Mono.error(new NotFoundException("Recurrence 不存在")));
    }

    private Mono<PlanningTaskEntity> ownedTask(UUID ownerId, UUID taskId) {
        return tasks.findById(taskId).filter(task -> task.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")));
    }

    private Mono<PlanningRecurrenceView> save(PlanningRecurrenceEntity old, Instant nextRunAt, boolean active) {
        Instant now = Instant.now();
        return recurrences.save(new PlanningRecurrenceEntity(old.id(), old.ownerId(), old.taskId(), old.rule(), old.mode(),
            old.timeZone(), nextRunAt, active, now, old.createdAt(), now, old.version())).map(this::view);
    }

    private PlanningRecurrenceView view(PlanningRecurrenceEntity recurrence) {
        return new PlanningRecurrenceView(recurrence.id(), recurrence.ownerId(), recurrence.taskId(), recurrence.rule(),
            recurrence.mode(), recurrence.timeZone(), recurrence.nextRunAt(), recurrence.active(), recurrence.lastRunAt(),
            recurrence.createdAt(), recurrence.updatedAt(), recurrence.version() == null ? 0 : recurrence.version());
    }
}
