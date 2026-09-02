package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningTaskAssignmentService implements PlanningTaskAssignmentService {
  private final PlanningTaskRepository tasks;
  private final PlanningProjectRepository projects;
  private final PlanningProjectMemberRepository members;
  private final PlanningTaskAssignmentRepository assignments;

  public PersistentPlanningTaskAssignmentService(PlanningTaskRepository t, PlanningProjectRepository p,
      PlanningProjectMemberRepository m, PlanningTaskAssignmentRepository a) {
    tasks = t;
    projects = p;
    members = m;
    assignments = a;
  }

  public Mono<PlanningTaskAssignmentView> assign(UUID actor, UUID taskId, AssignPlanningTaskRequest request) {
    return task(taskId).flatMap(task -> {
      if (task.projectId() == null) return Mono.error(new ConflictException("只有 Project 中的 Task 才能分配"));
      return requireRole(actor, task.projectId(), PlanningProjectMemberRole.ASSIGN)
          .then(members.findByProjectIdAndUserId(task.projectId(), request.assigneeId())
              .switchIfEmpty(Mono.error(new NotFoundException("Assignee 不是 Project 成员"))))
          .then(assignments.findByTaskIdAndAssigneeId(taskId, request.assigneeId())
              .flatMap(existing -> Mono.<PlanningTaskAssignmentEntity>error(new ConflictException("Task 已分配给该成员")))
              .switchIfEmpty(assignments.save(new PlanningTaskAssignmentEntity(null, taskId, request.assigneeId(),
                  actor, Instant.now()))))
          .map(this::view);
    });
  }

  public Flux<PlanningTaskAssignmentView> list(UUID actor, UUID taskId) {
    return task(taskId).flatMapMany(task -> visible(actor, task).thenMany(assignments.findAllByTaskId(taskId)))
        .map(this::view);
  }

  public Mono<Void> unassign(UUID actor, UUID taskId, UUID assigneeId) {
    return task(taskId).flatMap(task -> {
      if (task.projectId() == null) return Mono.error(new ConflictException("只有 Project 中的 Task 才能分配"));
      return requireRole(actor, task.projectId(), PlanningProjectMemberRole.ASSIGN)
          .then(assignments.findByTaskIdAndAssigneeId(taskId, assigneeId).flatMap(assignments::delete).then());
    });
  }

  private Mono<PlanningTaskEntity> task(UUID id) {
    return tasks.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")));
  }

  private Mono<Void> visible(UUID actor, PlanningTaskEntity task) {
    if (task.ownerId().equals(actor)) return Mono.empty();
    if (task.projectId() == null) return Mono.error(new NotFoundException("Task 不存在或无权访问"));
    return projects.findById(task.projectId())
        .flatMap(project -> project.ownerId().equals(actor)
            ? Mono.just(project)
            : members.findByProjectIdAndUserId(task.projectId(), actor).map(member -> project))
        .switchIfEmpty(Mono.error(new NotFoundException("Task 不存在或无权访问"))).then();
  }

  private Mono<Void> requireRole(UUID actor, UUID projectId, PlanningProjectMemberRole required) {
    if (projectId == null) return Mono.error(new NotFoundException("Task 不存在或无权访问"));
    return projects.findById(projectId)
        .flatMap(project -> project.ownerId().equals(actor)
            ? Mono.just(PlanningProjectMemberRole.MANAGE_PROJECT)
            : members.findByProjectIdAndUserId(projectId, actor).map(PlanningProjectMemberEntity::role))
        .filter(role -> role == PlanningProjectMemberRole.MANAGE_PROJECT || role == required)
        .switchIfEmpty(Mono.error(new NotFoundException("Project 不存在或无相应权限"))).then();
  }

  private PlanningTaskAssignmentView view(PlanningTaskAssignmentEntity x) {
    return new PlanningTaskAssignmentView(x.id(), x.taskId(), x.assigneeId(), x.assignedBy(), x.createdAt());
  }
}
