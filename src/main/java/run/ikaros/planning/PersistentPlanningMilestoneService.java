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
public class PersistentPlanningMilestoneService implements PlanningMilestoneService {
    private final PlanningMilestoneRepository milestones;
    private final PlanningGoalRepository goals;
    private final PlanningProjectRepository projects;
    public PersistentPlanningMilestoneService(PlanningMilestoneRepository milestones, PlanningGoalRepository goals, PlanningProjectRepository projects) {
        this.milestones = milestones; this.goals = goals; this.projects = projects;
    }
    @Override public Mono<PlanningMilestoneView> create(UUID ownerId, CreatePlanningMilestoneRequest request) {
        return goal(ownerId, request.goalId()).then(project(ownerId, request.projectId())).then(Mono.defer(() -> { Instant now = Instant.now();
            return milestones.save(new PlanningMilestoneEntity(null, ownerId, request.title().trim(), request.description(), request.goalId(), request.projectId(), request.dueAt(), PlanningMilestoneStatus.OPEN, null, now, now, null)).map(this::view); }));
    }
    @Override public Flux<PlanningMilestoneView> list(UUID ownerId, UUID goalId) { Flux<PlanningMilestoneEntity> source = goalId == null ? milestones.findAllByOwnerIdOrderByDueAtAsc(ownerId) : milestones.findAllByOwnerIdAndGoalIdOrderByDueAtAsc(ownerId, goalId); return source.take(100).map(this::view); }
    @Override public Mono<PlanningMilestoneView> update(UUID ownerId, UUID milestoneId, UpdatePlanningMilestoneRequest request) { return owned(ownerId, milestoneId).flatMap(old -> { if (old.status() == PlanningMilestoneStatus.ARCHIVED) return Mono.error(new ConflictException("已归档里程碑不能修改")); if ((old.version() == null ? 0 : old.version()) != request.expectedVersion()) return Mono.error(new PreconditionFailedException("If-Match 与 Milestone 当前版本不匹配")); return milestones.save(new PlanningMilestoneEntity(old.id(), old.ownerId(), request.title().trim(), request.description(), old.goalId(), old.projectId(), request.dueAt(), old.status(), old.achievedAt(), old.createdAt(), Instant.now(), old.version())); }).map(this::view); }
    @Override public Mono<PlanningMilestoneView> changeStatus(UUID ownerId, UUID milestoneId, PlanningMilestoneStatus status) { return changeStatusInternal(ownerId, milestoneId, status, null); }
    @Override public Mono<PlanningMilestoneView> changeStatus(UUID ownerId, UUID milestoneId, PlanningMilestoneStatus status, long expectedVersion) { return changeStatusInternal(ownerId, milestoneId, status, expectedVersion); }
    private Mono<PlanningMilestoneView> changeStatusInternal(UUID ownerId, UUID milestoneId, PlanningMilestoneStatus status, Long expectedVersion) { return owned(ownerId, milestoneId).flatMap(old -> { if (expectedVersion != null && (old.version() == null ? 0 : old.version()) != expectedVersion) return Mono.error(new PreconditionFailedException("If-Match 与 Milestone 当前版本不匹配")); if (old.status() == PlanningMilestoneStatus.ARCHIVED && status != PlanningMilestoneStatus.ARCHIVED) return Mono.error(new ConflictException("已归档里程碑不能恢复")); return milestones.save(new PlanningMilestoneEntity(old.id(), old.ownerId(), old.title(), old.description(), old.goalId(), old.projectId(), old.dueAt(), status, status == PlanningMilestoneStatus.ACHIEVED ? Instant.now() : old.achievedAt(), old.createdAt(), Instant.now(), old.version())); }).map(this::view); }
    private Mono<PlanningGoalEntity> goal(UUID ownerId, UUID id) { return goals.findById(id).filter(g -> g.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Goal 不存在"))); }
    private Mono<PlanningProjectEntity> project(UUID ownerId, UUID id) { return id == null ? Mono.empty() : projects.findById(id).filter(p -> p.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Project 不存在"))); }
    private Mono<PlanningMilestoneEntity> owned(UUID ownerId, UUID id) { return milestones.findById(id).filter(m -> m.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Milestone 不存在"))); }
    private PlanningMilestoneView view(PlanningMilestoneEntity m) { return new PlanningMilestoneView(m.id(), m.ownerId(), m.title(), m.description(), m.goalId(), m.projectId(), m.dueAt(), m.status(), m.achievedAt(), m.createdAt(), m.updatedAt(), m.version() == null ? 0 : m.version()); }
}
