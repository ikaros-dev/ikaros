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
  private final PlanningProjectRepository projects;
  private final PlanningProjectMemberRepository members;

  public PersistentPlanningRecurrenceService(PlanningRecurrenceRepository r, PlanningTaskRepository t,
      PlanningProjectRepository p, PlanningProjectMemberRepository m) {
    recurrences = r;
    tasks = t;
    projects = p;
    members = m;
  }

  @Override
  public Mono<PlanningRecurrenceView> create(UUID actor, UUID taskId, CreatePlanningRecurrenceRequest request) {
    return editableTask(actor, taskId).flatMap(task -> recurrences.findByTaskId(taskId)
        .flatMap(existing -> Mono.<PlanningRecurrenceEntity>error(new ConflictException("Task 已存在重复规则")))
        .switchIfEmpty(recurrences.save(new PlanningRecurrenceEntity(null, task.ownerId(), taskId,
            request.rule().trim(), request.mode() == null ? PlanningRecurrenceMode.FIXED_SCHEDULE : request.mode(),
            request.timeZone() == null ? "UTC" : request.timeZone(), request.nextRunAt(), true, null,
            Instant.now(), Instant.now(), null))))
        .map(this::view);
  }

  @Override
  public Mono<PlanningRecurrenceView> get(UUID actor, UUID taskId) {
    return visibleTask(actor, taskId).then(recurrence(taskId)).map(this::view);
  }

  @Override
  public Mono<PlanningRecurrenceView> setActive(UUID actor, UUID taskId, boolean active) {
    return editableTask(actor, taskId).then(recurrence(taskId)).flatMap(old -> save(old, old.nextRunAt(), active));
  }

  @Override
  public Mono<PlanningRecurrenceView> skip(UUID actor, UUID taskId, Instant nextRunAt) {
    return editableTask(actor, taskId).then(recurrence(taskId)).flatMap(old -> save(old, nextRunAt, old.active()));
  }

  private Mono<PlanningRecurrenceEntity> recurrence(UUID taskId) {
    return recurrences.findByTaskId(taskId).switchIfEmpty(Mono.error(new NotFoundException("Recurrence 不存在")));
  }

  private Mono<PlanningTaskEntity> editableTask(UUID actor, UUID taskId) {
    return task(taskId).flatMap(task -> task.ownerId().equals(actor) ? Mono.just(task)
        : role(actor, task.projectId()).filter(r -> r == PlanningProjectMemberRole.EDIT_TASK
            || r == PlanningProjectMemberRole.MANAGE_PROJECT)
            .switchIfEmpty(Mono.error(new NotFoundException("Task 不存在或无重复规则编辑权限"))).thenReturn(task));
  }

  private Mono<Void> visibleTask(UUID actor, UUID taskId) {
    return task(taskId).flatMap(task -> {
      if (task.ownerId().equals(actor)) return Mono.empty();
      return role(actor, task.projectId()).then();
    });
  }

  private Mono<PlanningTaskEntity> task(UUID taskId) {
    return tasks.findById(taskId).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")));
  }

  private Mono<PlanningProjectMemberRole> role(UUID actor, UUID projectId) {
    if (projectId == null) return Mono.error(new NotFoundException("Task 不存在或无权访问"));
    return projects.findById(projectId)
        .flatMap(project -> project.ownerId().equals(actor)
            ? Mono.just(PlanningProjectMemberRole.MANAGE_PROJECT)
            : members.findByProjectIdAndUserId(projectId, actor).map(PlanningProjectMemberEntity::role))
        .switchIfEmpty(Mono.error(new NotFoundException("Project 不存在或无权访问")));
  }

  private Mono<PlanningRecurrenceView> save(PlanningRecurrenceEntity old, Instant nextRunAt, boolean active) {
    Instant now = Instant.now();
    return recurrences.save(new PlanningRecurrenceEntity(old.id(), old.ownerId(), old.taskId(), old.rule(), old.mode(),
        old.timeZone(), nextRunAt, active, now, old.createdAt(), now, old.version())).map(this::view);
  }

  private PlanningRecurrenceView view(PlanningRecurrenceEntity r) {
    return new PlanningRecurrenceView(r.id(), r.ownerId(), r.taskId(), r.rule(), r.mode(), r.timeZone(),
        r.nextRunAt(), r.active(), r.lastRunAt(), r.createdAt(), r.updatedAt(), r.version() == null ? 0 : r.version());
  }
}
