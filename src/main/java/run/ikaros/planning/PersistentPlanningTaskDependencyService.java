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
    public PersistentPlanningTaskDependencyService(PlanningTaskDependencyRepository dependencies, PlanningTaskRepository tasks) {
        this.dependencies = dependencies; this.tasks = tasks;
    }
    @Override public Mono<PlanningTaskDependencyView> create(UUID ownerId, UUID taskId, CreatePlanningTaskDependencyRequest request) {
        if (taskId.equals(request.dependsOnTaskId())) return Mono.error(new ConflictException("Task 不能依赖自身"));
        return task(ownerId, taskId).then(task(ownerId, request.dependsOnTaskId())).then(dependencies.findByTaskIdAndDependsOnTaskId(taskId, request.dependsOnTaskId())
            .flatMap(existing -> Mono.<PlanningTaskDependencyView>error(new ConflictException("依赖关系已存在")))
            .switchIfEmpty(Mono.defer(() -> reaches(request.dependsOnTaskId(), taskId, new HashSet<>()).flatMap(cycle -> cycle
                ? Mono.error(new ConflictException("不能创建循环依赖"))
                : dependencies.save(new PlanningTaskDependencyEntity(null, taskId, request.dependsOnTaskId(),
                    request.type() == null ? PlanningTaskDependencyType.BLOCKED_BY : request.type(), Instant.now())).map(this::view)))));
    }
    @Override public Flux<PlanningTaskDependencyView> list(UUID ownerId, UUID taskId) { return task(ownerId, taskId).thenMany(dependencies.findAllByTaskId(taskId)).map(this::view); }
    @Override public Mono<Void> delete(UUID ownerId, UUID taskId, UUID dependsOnTaskId) { return task(ownerId, taskId).then(task(ownerId, dependsOnTaskId))
        .then(dependencies.findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId).flatMap(dependencies::delete).then()).then(); }
    private Mono<PlanningTaskEntity> task(UUID ownerId, UUID taskId) { return tasks.findById(taskId).filter(t -> t.ownerId().equals(ownerId))
        .switchIfEmpty(Mono.error(new NotFoundException("Task 不存在"))); }
    private Mono<Boolean> reaches(UUID current, UUID target, Set<UUID> visited) { if (!visited.add(current)) return Mono.just(false); return dependencies.findAllByTaskId(current)
        .map(PlanningTaskDependencyEntity::dependsOnTaskId).concatMap(next -> next.equals(target) ? Mono.just(true) : reaches(next, target, visited)).any(Boolean::booleanValue); }
    private PlanningTaskDependencyView view(PlanningTaskDependencyEntity dependency) { return new PlanningTaskDependencyView(dependency.id(), dependency.taskId(), dependency.dependsOnTaskId(), dependency.type(), dependency.createdAt()); }
}
