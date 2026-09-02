package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningReminderService implements PlanningReminderService {
    private final PlanningReminderRepository reminders;
    private final PlanningTaskRepository tasks;

    public PersistentPlanningReminderService(PlanningReminderRepository reminders, PlanningTaskRepository tasks) {
        this.reminders = reminders; this.tasks = tasks;
    }

    @Override public Mono<PlanningReminderView> create(UUID ownerId, CreatePlanningReminderRequest request) {
        if (request.targetType() == PlanningReminderTargetType.TASK) return task(ownerId, request.targetId()).then(saveNew(ownerId, request));
        return saveNew(ownerId, request);
    }

    @Override public Flux<PlanningReminderView> list(UUID ownerId) { return reminders.findAllByOwnerIdOrderByTriggerAt(ownerId).map(this::view); }

    @Override public Mono<PlanningReminderView> acknowledge(UUID ownerId, UUID reminderId) {
        return owned(ownerId, reminderId).flatMap(old -> {
            if (old.status() == PlanningReminderStatus.CANCELLED) return Mono.error(new ConflictException("已取消的提醒不能确认"));
            return save(old, PlanningReminderStatus.ACKNOWLEDGED, old.snoozedUntil(), old.firedAt(), Instant.now());
        });
    }

    @Override public Mono<PlanningReminderView> snooze(UUID ownerId, UUID reminderId, Instant until) {
        if (until == null || !until.isAfter(Instant.now())) return Mono.error(new ConflictException("延后时间必须晚于当前时间"));
        return owned(ownerId, reminderId).flatMap(old -> {
            if (old.status() == PlanningReminderStatus.ACKNOWLEDGED || old.status() == PlanningReminderStatus.CANCELLED) return Mono.error(new ConflictException("当前提醒不能延后"));
            return save(old, PlanningReminderStatus.SNOOZED, until, old.firedAt(), old.acknowledgedAt());
        });
    }

    @Override public Mono<PlanningReminderView> cancel(UUID ownerId, UUID reminderId) {
        return owned(ownerId, reminderId).flatMap(old -> save(old, PlanningReminderStatus.CANCELLED, old.snoozedUntil(), old.firedAt(), old.acknowledgedAt()));
    }

    private Mono<PlanningReminderView> saveNew(UUID ownerId, CreatePlanningReminderRequest request) {
        Instant now = Instant.now();
        return reminders.save(new PlanningReminderEntity(null, ownerId, request.targetType(), request.targetId(), request.triggerAt(),
            request.timeZone() == null ? "UTC" : request.timeZone(), request.channel() == null ? "IN_APP" : request.channel(),
            PlanningReminderStatus.SCHEDULED, null, null, null, now, now, null)).map(this::view);
    }
    private Mono<PlanningTaskEntity> task(UUID ownerId, UUID taskId) { return tasks.findById(taskId).filter(task -> task.ownerId().equals(ownerId))
        .switchIfEmpty(Mono.error(new NotFoundException("Task 不存在"))); }
    private Mono<PlanningReminderEntity> owned(UUID ownerId, UUID id) { return reminders.findById(id).filter(reminder -> reminder.ownerId().equals(ownerId))
        .switchIfEmpty(Mono.error(new NotFoundException("Reminder 不存在"))); }
    private Mono<PlanningReminderView> save(PlanningReminderEntity old, PlanningReminderStatus status, Instant snoozedUntil,
        Instant firedAt, Instant acknowledgedAt) { return reminders.save(new PlanningReminderEntity(old.id(), old.ownerId(), old.targetType(), old.targetId(),
        old.triggerAt(), old.timeZone(), old.channel(), status, snoozedUntil, firedAt, acknowledgedAt, old.createdAt(), Instant.now(), old.version())).map(this::view); }
    private PlanningReminderView view(PlanningReminderEntity reminder) { return new PlanningReminderView(reminder.id(), reminder.ownerId(), reminder.targetType(), reminder.targetId(),
        reminder.triggerAt(), reminder.timeZone(), reminder.channel(), reminder.status(), reminder.snoozedUntil(), reminder.firedAt(), reminder.acknowledgedAt(), reminder.createdAt(), reminder.updatedAt(), reminder.version() == null ? 0 : reminder.version()); }
}
