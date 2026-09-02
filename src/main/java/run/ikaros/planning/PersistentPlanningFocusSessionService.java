package run.ikaros.planning;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningFocusSessionService implements PlanningFocusSessionService {
    private final PlanningFocusSessionRepository sessions;
    private final PlanningTaskRepository tasks;
    private final PlanningTimeEntryRepository entries;
    public PersistentPlanningFocusSessionService(PlanningFocusSessionRepository sessions, PlanningTaskRepository tasks,
        PlanningTimeEntryRepository entries) { this.sessions = sessions; this.tasks = tasks; this.entries = entries; }
    @Override public Mono<PlanningFocusSessionView> start(UUID ownerId, StartPlanningFocusSessionRequest request) {
        return task(ownerId, request.taskId()).then(Mono.defer(() -> { Instant now = Instant.now();
            return sessions.save(new PlanningFocusSessionEntity(null, ownerId, request.taskId(), request.mode() == null ? PlanningFocusMode.FREEFORM : request.mode(),
                PlanningFocusSessionStatus.RUNNING, request.plannedMinutes(), null, now, null, null, now, null)).map(this::view); }));
    }
    @Override public Flux<PlanningFocusSessionView> list(UUID ownerId) { return sessions.findAllByOwnerIdOrderByStartedAtDesc(ownerId).map(this::view); }
    @Override public Mono<PlanningFocusSessionView> complete(UUID ownerId, UUID sessionId, CompletePlanningFocusSessionRequest request) {
        return owned(ownerId, sessionId).flatMap(old -> { if (old.status() != PlanningFocusSessionStatus.RUNNING) return Mono.error(new ConflictException("当前专注会话不能完成"));
            Instant ended = Instant.now(); int minutes = request.actualMinutes() == null ? Math.max(1, (int) Duration.between(old.startedAt(), ended).toMinutes()) : request.actualMinutes();
            return sessions.save(new PlanningFocusSessionEntity(old.id(), old.ownerId(), old.taskId(), old.mode(), PlanningFocusSessionStatus.COMPLETED,
                old.plannedMinutes(), minutes, old.startedAt(), ended, request.note(), old.createdAt(), old.version())).flatMap(saved -> old.taskId() == null ? Mono.just(saved)
                : entries.save(new PlanningTimeEntryEntity(null, ownerId, old.taskId(), minutes, old.startedAt(), ended, PlanningTimeEntrySource.FOCUS, request.note(), ended)).thenReturn(saved));
        }).map(this::view);
    }
    @Override public Mono<PlanningFocusSessionView> cancel(UUID ownerId, UUID sessionId) { return owned(ownerId, sessionId).flatMap(old -> { if (old.status() != PlanningFocusSessionStatus.RUNNING) return Mono.error(new ConflictException("当前专注会话不能取消"));
        return sessions.save(new PlanningFocusSessionEntity(old.id(), old.ownerId(), old.taskId(), old.mode(), PlanningFocusSessionStatus.CANCELLED, old.plannedMinutes(), null, old.startedAt(), Instant.now(), old.note(), old.createdAt(), old.version())); }).map(this::view); }
    private Mono<PlanningTaskEntity> task(UUID ownerId, UUID taskId) { return taskId == null ? Mono.empty() : tasks.findById(taskId).filter(t -> t.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在"))); }
    private Mono<PlanningFocusSessionEntity> owned(UUID ownerId, UUID id) { return sessions.findById(id).filter(s -> s.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Focus Session 不存在"))); }
    private PlanningFocusSessionView view(PlanningFocusSessionEntity s) { return new PlanningFocusSessionView(s.id(), s.ownerId(), s.taskId(), s.mode(), s.status(), s.plannedMinutes(), s.actualMinutes(), s.startedAt(), s.endedAt(), s.note(), s.createdAt(), s.version() == null ? 0 : s.version()); }
}
