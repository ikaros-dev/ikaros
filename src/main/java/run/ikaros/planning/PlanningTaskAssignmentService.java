package run.ikaros.planning; import java.util.UUID; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
public interface PlanningTaskAssignmentService {Mono<PlanningTaskAssignmentView> assign(UUID ownerId,UUID taskId,AssignPlanningTaskRequest request);Flux<PlanningTaskAssignmentView> list(UUID ownerId,UUID taskId);Mono<Void> unassign(UUID ownerId,UUID taskId,UUID assigneeId);}
