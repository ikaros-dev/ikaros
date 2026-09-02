package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/tasks/{taskId}/dependencies")
public class PlanningTaskDependencyController {
    private final PlanningTaskDependencyService service;
    public PlanningTaskDependencyController(PlanningTaskDependencyService service) { this.service = service; }
    @PostMapping public Mono<PlanningTaskDependencyView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId, @Valid @RequestBody CreatePlanningTaskDependencyRequest request) { return service.create(ownerId, taskId, request); }
    @GetMapping public Flux<PlanningTaskDependencyView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId) { return service.list(ownerId, taskId); }
    @DeleteMapping("/{dependsOnTaskId}") public Mono<Void> delete(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId, @PathVariable UUID dependsOnTaskId) { return service.delete(ownerId, taskId, dependsOnTaskId); }
}
