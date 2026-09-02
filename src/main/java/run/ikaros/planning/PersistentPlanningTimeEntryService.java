package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningTimeEntryService implements PlanningTimeEntryService {
  private final PlanningTimeEntryRepository entries;
  private final PlanningTaskRepository tasks;
  private final PlanningProjectRepository projects;
  private final PlanningProjectMemberRepository members;

  public PersistentPlanningTimeEntryService(PlanningTimeEntryRepository e, PlanningTaskRepository t,
      PlanningProjectRepository p, PlanningProjectMemberRepository m) {
    entries = e;
    tasks = t;
    projects = p;
    members = m;
  }

  @Override
  public Mono<PlanningTimeEntryView> create(UUID actor, UUID taskId, CreatePlanningTimeEntryRequest request) {
    return task(taskId).flatMap(task -> visible(actor, task).then(Mono.defer(() -> {
      if (request.startedAt() != null && request.endedAt() != null && !request.endedAt().isAfter(request.startedAt())) {
        return Mono.error(new ConflictException("实际耗时结束时间必须晚于开始时间"));
      }
      Instant now = Instant.now();
      return entries.save(new PlanningTimeEntryEntity(null, actor, taskId, request.durationMinutes(), request.startedAt(),
          request.endedAt(), request.source() == null ? PlanningTimeEntrySource.MANUAL : request.source(),
          request.note(), now));
    }))).map(this::view);
  }

  @Override
  public Flux<PlanningTimeEntryView> list(UUID actor, UUID taskId) {
    return task(taskId).flatMapMany(task -> visible(actor, task)
        .thenMany(entries.findAllByTaskIdOrderByCreatedAtDesc(taskId))).map(this::view);
  }

  private Mono<PlanningTaskEntity> task(UUID taskId) {
    return tasks.findById(taskId).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")));
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

  private PlanningTimeEntryView view(PlanningTimeEntryEntity entry) {
    return new PlanningTimeEntryView(entry.id(), entry.ownerId(), entry.taskId(), entry.durationMinutes(),
        entry.startedAt(), entry.endedAt(), entry.source(), entry.note(), entry.createdAt());
  }
}
