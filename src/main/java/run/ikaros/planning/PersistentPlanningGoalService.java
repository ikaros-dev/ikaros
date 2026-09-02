package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;

@Service
public class PersistentPlanningGoalService implements PlanningGoalService {
    private final PlanningGoalRepository goals;
    private final PlanningGoalTaskRepository goalTasks;
    private final PlanningTaskRepository tasks;

    public PersistentPlanningGoalService(PlanningGoalRepository goals, PlanningGoalTaskRepository goalTasks,
        PlanningTaskRepository tasks) { this.goals = goals; this.goalTasks = goalTasks; this.tasks = tasks; }

    @Override public Mono<PlanningGoalView> create(UUID ownerId, CreatePlanningGoalRequest request) {
        validateDates(request.startAt(), request.deadline());
        Instant now = Instant.now();
        return goals.save(new PlanningGoalEntity(null, ownerId, request.title().trim(), request.description(),
            request.type() == null ? PlanningGoalType.OUTCOME : request.type(), PlanningGoalStatus.ACTIVE, 0d,
            request.startAt(), request.deadline(), null, now, now, null)).map(this::view);
    }
    @Override public Flux<PlanningGoalView> list(UUID ownerId) { return goals.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId).map(this::view); }
    @Override public Mono<PlanningGoalView> update(UUID ownerId, UUID goalId, UpdatePlanningGoalRequest request) {
        validateDates(request.startAt(), request.deadline());
        return owned(ownerId, goalId).flatMap(old -> { check(old, request.expectedVersion());
            if (old.status() == PlanningGoalStatus.ARCHIVED) return Mono.error(new ConflictException("已归档目标不能修改"));
            return goals.save(new PlanningGoalEntity(old.id(), old.ownerId(), request.title().trim(), request.description(),
                request.type() == null ? old.type() : request.type(), old.status(), old.progress(), request.startAt(),
                request.deadline(), old.completedAt(), old.createdAt(), Instant.now(), old.version()));
        }).map(this::view);
    }
    @Override public Mono<PlanningGoalView> updateProgress(UUID ownerId, UUID goalId, UpdatePlanningGoalProgressRequest request) {
        return owned(ownerId, goalId).flatMap(old -> { check(old, request.expectedVersion());
            if (old.status() == PlanningGoalStatus.ARCHIVED) return Mono.error(new ConflictException("已归档目标不能修改"));
            PlanningGoalStatus status = request.progress() >= 1d ? PlanningGoalStatus.COMPLETED : old.status();
            return goals.save(new PlanningGoalEntity(old.id(), old.ownerId(), old.title(), old.description(), old.type(), status,
                request.progress(), old.startAt(), old.deadline(), status == PlanningGoalStatus.COMPLETED ? Instant.now() : old.completedAt(),
                old.createdAt(), Instant.now(), old.version()));
        }).map(this::view);
    }
    @Override public Mono<PlanningGoalView> changeStatus(UUID ownerId, UUID goalId, PlanningGoalStatus status) {
        return changeStatusInternal(ownerId, goalId, status, null);
    }
    @Override public Mono<PlanningGoalView> changeStatus(UUID ownerId, UUID goalId, PlanningGoalStatus status, long expectedVersion) {
        return changeStatusInternal(ownerId, goalId, status, expectedVersion);
    }
    private Mono<PlanningGoalView> changeStatusInternal(UUID ownerId, UUID goalId, PlanningGoalStatus status, Long expectedVersion) {
        return owned(ownerId, goalId).flatMap(old -> { if (expectedVersion != null) check(old, expectedVersion);
            if (old.status() == PlanningGoalStatus.ARCHIVED && status != PlanningGoalStatus.ARCHIVED)
                return Mono.error(new ConflictException("已归档目标不能恢复"));
            return goals.save(new PlanningGoalEntity(old.id(), old.ownerId(), old.title(), old.description(), old.type(), status, old.progress(),
                old.startAt(), old.deadline(), status == PlanningGoalStatus.COMPLETED ? Instant.now() : old.completedAt(), old.createdAt(), Instant.now(), old.version()));
        }).map(this::view);
    }
    @Override public Mono<Void> attachTask(UUID ownerId, UUID goalId, UUID taskId) { return owned(ownerId, goalId).then(task(ownerId, taskId))
        .then(goalTasks.findByGoalIdAndTaskId(goalId, taskId).switchIfEmpty(goalTasks.save(new PlanningGoalTaskEntity(null, goalId, taskId, Instant.now()))).then()).then(); }
    @Override public Mono<Void> detachTask(UUID ownerId, UUID goalId, UUID taskId) { return owned(ownerId, goalId).then(task(ownerId, taskId))
        .then(goalTasks.findByGoalIdAndTaskId(goalId, taskId).flatMap(goalTasks::delete).then()).then(); }
    private Mono<PlanningGoalEntity> owned(UUID ownerId, UUID goalId) { return goals.findById(goalId).filter(goal -> goal.ownerId().equals(ownerId))
        .switchIfEmpty(Mono.error(new NotFoundException("Goal 不存在"))); }
    private Mono<PlanningTaskEntity> task(UUID ownerId, UUID taskId) { return tasks.findById(taskId).filter(task -> task.ownerId().equals(ownerId))
        .switchIfEmpty(Mono.error(new NotFoundException("Task 不存在"))); }
    private void check(PlanningGoalEntity goal, long expected) { if ((goal.version() == null ? 0 : goal.version()) != expected) throw new PreconditionFailedException("If-Match 与 Goal 当前版本不匹配"); }
    private void validateDates(Instant start, Instant deadline) { if (start != null && deadline != null && !deadline.isAfter(start)) throw new ConflictException("目标截止时间必须晚于开始时间"); }
    private PlanningGoalView view(PlanningGoalEntity goal) { return new PlanningGoalView(goal.id(), goal.ownerId(), goal.title(), goal.description(), goal.type(), goal.status(),
        goal.progress() == null ? 0 : goal.progress(), goal.startAt(), goal.deadline(), goal.completedAt(), goal.createdAt(), goal.updatedAt(), goal.version() == null ? 0 : goal.version()); }
}
