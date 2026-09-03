package run.ikaros.planning;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningTaskDependencyService implements PlanningTaskDependencyService {
  private final PlanningTaskDependencyRepository dependencies;
  private final PlanningTaskRepository tasks;
  private final PlanningProjectRepository projects;
  private final PlanningProjectMemberRepository members;

  public PersistentPlanningTaskDependencyService(PlanningTaskDependencyRepository d, PlanningTaskRepository t,
      PlanningProjectRepository p, PlanningProjectMemberRepository m) {
    dependencies = d;
    tasks = t;
    projects = p;
    members = m;
  }

  @Override
  public Mono<PlanningTaskDependencyView> create(UUID actor, UUID taskId, CreatePlanningTaskDependencyRequest request) {
    if (taskId.equals(request.dependsOnTaskId())) return Mono.error(new ConflictException("Task 不能依赖自身"));
    return task(taskId)
        .flatMap(source -> requireEdit(actor, source)
            .then(task(request.dependsOnTaskId()))
            .flatMap(target -> visible(actor, target)
                .then(ensureDependencyAbsent(taskId, request.dependsOnTaskId(), request.type()))))
        .map(this::view);
  }

  @Override
  public Flux<PlanningTaskDependencyView> list(UUID actor, UUID taskId) {
    return task(taskId).flatMapMany(task -> visible(actor, task)
        .thenMany(dependencies.findAllByTaskId(taskId).take(100))).map(this::view);
  }

  @Override
  public Mono<Void> delete(UUID actor, UUID taskId, UUID dependsOnTaskId) {
    return task(taskId).flatMap(source -> requireEdit(actor, source)
        .then(task(dependsOnTaskId)).flatMap(target -> visible(actor, target))
        .then(dependencies.findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId)
            .flatMap(dependencies::delete).then())).then();
  }

  private Mono<PlanningTaskEntity> task(UUID id) {
    return tasks.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")));
  }

  private Mono<Void> requireEdit(UUID actor, PlanningTaskEntity task) {
    if (task.ownerId().equals(actor)) return Mono.empty();
    return role(actor, task.projectId()).filter(r -> r == PlanningProjectMemberRole.EDIT_TASK
        || r == PlanningProjectMemberRole.MANAGE_PROJECT)
        .switchIfEmpty(Mono.error(new NotFoundException("Task 不存在或无任务编辑权限"))).then();
  }

  private Mono<Void> visible(UUID actor, PlanningTaskEntity task) {
    if (task.ownerId().equals(actor)) return Mono.empty();
    return role(actor, task.projectId()).then();
  }

  private Mono<PlanningProjectMemberRole> role(UUID actor, UUID projectId) {
    if (projectId == null) return Mono.error(new NotFoundException("Task 不存在或无权访问"));
    return projects.findById(projectId)
        .flatMap(project -> project.ownerId().equals(actor)
            ? Mono.just(PlanningProjectMemberRole.MANAGE_PROJECT)
            : members.findByProjectIdAndUserId(projectId, actor).map(PlanningProjectMemberEntity::role))
        .switchIfEmpty(Mono.error(new NotFoundException("Project 不存在或无权访问")));
  }

  private Mono<Boolean> reaches(UUID current, UUID target, Set<UUID> visited) {
    if (!visited.add(current)) return Mono.just(false);
    return dependencies.findAllByTaskId(current).map(PlanningTaskDependencyEntity::dependsOnTaskId)
        .concatMap(next -> next.equals(target) ? Mono.just(true) : reaches(next, target, visited))
        .any(Boolean::booleanValue);
  }

  private Mono<PlanningTaskDependencyEntity> ensureDependencyAbsent(UUID taskId, UUID dependsOnTaskId,
      PlanningTaskDependencyType type) {
    return dependencies.findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId)
        .flatMap(existing -> Mono.<PlanningTaskDependencyEntity>error(new ConflictException("依赖关系已存在")))
        .switchIfEmpty(Mono.defer(() -> reaches(dependsOnTaskId, taskId, new HashSet<>())
            .flatMap(cycle -> cycle
                ? Mono.error(new ConflictException("不能创建循环依赖"))
                : dependencies.save(new PlanningTaskDependencyEntity(null, taskId, dependsOnTaskId,
                    type == null ? PlanningTaskDependencyType.BLOCKED_BY : type, Instant.now())))));
  }

  private PlanningTaskDependencyView view(PlanningTaskDependencyEntity d) {
    return new PlanningTaskDependencyView(d.id(), d.taskId(), d.dependsOnTaskId(), d.type(), d.createdAt());
  }
}
