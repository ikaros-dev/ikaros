package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/goals")
public class PlanningGoalController {
    private final PlanningGoalService service;
    public PlanningGoalController(PlanningGoalService service) { this.service = service; }
    @PostMapping public Mono<PlanningGoalView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody CreatePlanningGoalRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningGoalView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @PatchMapping("/{goalId}") public Mono<PlanningGoalView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID goalId, @Valid @RequestBody UpdatePlanningGoalRequest request) { return service.update(ownerId, goalId, request); }
    @PutMapping("/{goalId}/progress") public Mono<PlanningGoalView> progress(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID goalId, @Valid @RequestBody UpdatePlanningGoalProgressRequest request) { return service.updateProgress(ownerId, goalId, request); }
    @PostMapping("/{goalId}/status/{status}") public Mono<PlanningGoalView> status(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID goalId, @PathVariable PlanningGoalStatus status) { return service.changeStatus(ownerId, goalId, status); }
    @PutMapping("/{goalId}/tasks/{taskId}") public Mono<Void> attachTask(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID goalId, @PathVariable UUID taskId) { return service.attachTask(ownerId, goalId, taskId); }
    @DeleteMapping("/{goalId}/tasks/{taskId}") public Mono<Void> detachTask(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID goalId, @PathVariable UUID taskId) { return service.detachTask(ownerId, goalId, taskId); }
}
