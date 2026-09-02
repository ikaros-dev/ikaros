package run.ikaros.planning;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;

@Service
public class PersistentPlanningTaskService implements PlanningTaskService {
  private final PlanningTaskRepository repository;
  private final PlanningProjectSectionRepository sections;
  private final PlanningProjectRepository projects;
  private final PlanningProjectMemberRepository members;

  public PersistentPlanningTaskService(PlanningTaskRepository repository, PlanningProjectSectionRepository sections,
      PlanningProjectRepository projects, PlanningProjectMemberRepository members) {
    this.repository = repository;
    this.sections = sections;
    this.projects = projects;
    this.members = members;
  }

  @Override
  public Mono<PlanningTaskView> create(UUID actor, CreatePlanningTaskRequest req) {
    validateSchedule(req.scheduledStart(), req.scheduledEnd());
    validateDuration(req.estimatedDurationMinutes());
    return requireProjectRole(actor, req.projectId(), PlanningProjectMemberRole.EDIT_TASK)
        .then(validSection(req.projectId(), req.sectionId()))
        .then(validParent(actor, req.projectId(), req.parentTaskId()))
        .then(Mono.defer(() -> {
          Instant now = Instant.now();
          return repository.save(new PlanningTaskEntity(null, actor, req.title().trim(), req.description(),
              PlanningTaskStatus.INBOX, req.priority() == null ? PlanningTaskPriority.NONE : req.priority(),
              req.important(), req.urgent(), req.scheduledStart(), req.scheduledEnd(), req.deadline(),
              req.estimatedDurationMinutes(), req.projectId(), req.sectionId(), req.parentTaskId(), null, now, now, null));
        }))
        .map(this::view);
  }

  @Override
  public Flux<PlanningTaskView> list(UUID actor, PlanningTaskStatus status) {
    return accessible(actor).filter(t -> status == null || t.status() == status).map(this::view);
  }

  @Override
  public Flux<PlanningTaskView> today(UUID actor, ZoneId zoneId) {
    ZonedDateTime start = Instant.now().atZone(zoneId).toLocalDate().atStartOfDay(zoneId);
    Instant from = start.toInstant();
    Instant to = start.plusDays(1).toInstant();
    return accessible(actor).filter(t -> visible(t) && inWindow(t, from, to)).map(this::view);
  }

  @Override
  public Flux<PlanningTaskView> upcoming(UUID actor, Instant from) {
    Instant now = from == null ? Instant.now() : from;
    return accessible(actor).filter(t -> visible(t)
        && ((t.scheduledStart() != null && !t.scheduledStart().isBefore(now))
            || (t.deadline() != null && !t.deadline().isBefore(now))))
        .map(this::view);
  }

  @Override
  public Flux<PlanningTaskView> filter(UUID actor, PlanningTaskStatus status, PlanningTaskPriority priority,
      Instant from, Instant to, boolean overdue) {
    Instant now = Instant.now();
    return accessible(actor).filter(t -> (status == null || t.status() == status)
        && (priority == null || t.priority() == priority)
        && (from == null || t.deadline() == null || !t.deadline().isBefore(from))
        && (to == null || t.deadline() == null || t.deadline().isBefore(to))
        && (!overdue || (t.deadline() != null && t.deadline().isBefore(now) && visible(t))))
        .map(this::view);
  }

  @Override
  public Flux<PlanningTaskView> eisenhower(UUID actor, boolean important, boolean urgent) {
    return accessible(actor).filter(t -> visible(t) && t.important() == important && t.urgent() == urgent)
        .map(this::view);
  }

  @Override
  public Mono<PlanningTaskView> update(UUID actor, UUID id, UpdatePlanningTaskRequest req) {
    return editable(actor, id).flatMap(old -> {
      check(old, req.expectedVersion());
      validateSchedule(req.scheduledStart(), req.scheduledEnd());
      validateDuration(req.estimatedDurationMinutes());
      return validSection(old.projectId(), req.sectionId()).then(repository.save(new PlanningTaskEntity(old.id(),
          old.ownerId(), req.title().trim(), req.description(), old.status(),
          req.priority() == null ? PlanningTaskPriority.NONE : req.priority(), req.important(), req.urgent(),
          req.scheduledStart(), req.scheduledEnd(), req.deadline(), req.estimatedDurationMinutes(), old.projectId(),
          req.sectionId(), old.parentTaskId(), old.completedAt(), old.createdAt(), Instant.now(), old.version())));
    }).map(this::view);
  }

  @Override
  public Mono<PlanningTaskView> changeStatus(UUID actor, UUID id, PlanningTaskStatus status) {
    return changeStatusInternal(actor, id, status, null);
  }

  @Override
  public Mono<PlanningTaskView> changeStatus(UUID actor, UUID id, PlanningTaskStatus status, long expectedVersion) {
    return changeStatusInternal(actor, id, status, expectedVersion);
  }

  private Mono<PlanningTaskView> changeStatusInternal(UUID actor, UUID id, PlanningTaskStatus status, Long expectedVersion) {
    return editable(actor, id).flatMap(old -> {
      if (expectedVersion != null) check(old, expectedVersion);
      if (old.status() == PlanningTaskStatus.ARCHIVED) {
        return Mono.error(new ConflictException("已归档任务不能修改"));
      }
      Instant completed = status == PlanningTaskStatus.COMPLETED ? Instant.now() : null;
      return repository.save(new PlanningTaskEntity(old.id(), old.ownerId(), old.title(), old.description(), status,
          old.priority(), old.important(), old.urgent(), old.scheduledStart(), old.scheduledEnd(), old.deadline(),
          old.estimatedDurationMinutes(), old.projectId(), old.sectionId(), old.parentTaskId(), completed,
          old.createdAt(), Instant.now(), old.version()));
    }).map(this::view);
  }

  private Flux<PlanningTaskEntity> accessible(UUID actor) {
    Flux<PlanningTaskEntity> owned = repository.findAllByOwnerIdOrderByCreatedAtDesc(actor);
    Flux<PlanningTaskEntity> shared = members.findAllByUserId(actor)
        .map(PlanningProjectMemberEntity::projectId)
        .flatMap(repository::findAllByProjectIdOrderByCreatedAtDesc);
    return Flux.concat(owned, shared).distinct(PlanningTaskEntity::id);
  }

  private Mono<PlanningTaskEntity> editable(UUID actor, UUID id) {
    return repository.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")))
        .flatMap(task -> task.ownerId().equals(actor)
            ? Mono.just(task)
            : requireProjectRole(actor, task.projectId(), PlanningProjectMemberRole.EDIT_TASK).thenReturn(task));
  }

  private Mono<Void> requireProjectRole(UUID actor, UUID projectId, PlanningProjectMemberRole required) {
    if (projectId == null) {
      return Mono.empty();
    }
    return projects.findById(projectId)
        .flatMap(project -> project.ownerId().equals(actor)
            ? Mono.just(PlanningProjectMemberRole.MANAGE_PROJECT)
            : members.findByProjectIdAndUserId(projectId, actor).map(PlanningProjectMemberEntity::role))
        .filter(role -> role == PlanningProjectMemberRole.MANAGE_PROJECT || role == required)
        .switchIfEmpty(Mono.error(new NotFoundException("Project 不存在或无任务编辑权限")))
        .then();
  }

  private Mono<Void> validSection(UUID projectId, UUID sectionId) {
    if (sectionId == null) return Mono.empty();
    return sections.findById(sectionId).filter(s -> projectId != null && s.projectId().equals(projectId))
        .switchIfEmpty(Mono.error(new ConflictException("Section 不属于该 Project"))).then();
  }

  private Mono<Void> validParent(UUID actor, UUID projectId, UUID parentTaskId) {
    if (parentTaskId == null) return Mono.empty();
    return repository.findById(parentTaskId)
        .switchIfEmpty(Mono.error(new ConflictException("Parent Task 不存在")))
        .flatMap(parent -> {
          if (!java.util.Objects.equals(projectId, parent.projectId())) {
            return Mono.error(new ConflictException("Parent Task 必须属于同一 Project"));
          }
          return parent.ownerId().equals(actor) ? Mono.empty() : requireProjectMember(actor, projectId);
        });
  }

  private Mono<Void> requireProjectMember(UUID actor, UUID projectId) {
    return projects.findById(projectId)
        .flatMap(project -> project.ownerId().equals(actor)
            ? Mono.just(project)
            : members.findByProjectIdAndUserId(projectId, actor).map(member -> project))
        .switchIfEmpty(Mono.error(new NotFoundException("Project 不存在或无权访问"))).then();
  }

  private void check(PlanningTaskEntity t, long expected) {
    long actual = t.version() == null ? 0 : t.version();
    if (actual != expected) throw new PreconditionFailedException("If-Match 与 Task 当前版本不匹配");
  }

  private void validateSchedule(Instant start, Instant end) {
    if (start != null && end != null && !end.isAfter(start)) throw new ConflictException("计划结束时间必须晚于开始时间");
  }

  private void validateDuration(Integer minutes) {
    if (minutes != null && minutes <= 0) throw new ConflictException("预计时长必须大于 0");
  }

  private boolean visible(PlanningTaskEntity t) {
    return t.status() != PlanningTaskStatus.COMPLETED && t.status() != PlanningTaskStatus.CANCELLED
        && t.status() != PlanningTaskStatus.ARCHIVED;
  }

  private boolean inWindow(PlanningTaskEntity t, Instant from, Instant to) {
    return (t.scheduledStart() != null && !t.scheduledStart().isBefore(from) && t.scheduledStart().isBefore(to))
        || (t.deadline() != null && !t.deadline().isBefore(from) && t.deadline().isBefore(to));
  }

  private PlanningTaskView view(PlanningTaskEntity t) {
    return new PlanningTaskView(t.id(), t.ownerId(), t.title(), t.description(), t.status(), t.priority(),
        t.important(), t.urgent(), t.scheduledStart(), t.scheduledEnd(), t.deadline(), t.estimatedDurationMinutes(),
        t.projectId(), t.sectionId(), t.parentTaskId(), t.completedAt(), t.createdAt(), t.updatedAt(),
        t.version() == null ? 0 : t.version());
  }
}
