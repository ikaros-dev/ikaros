package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/milestones")
public class PlanningMilestoneController {
    private final PlanningMilestoneService service;
    public PlanningMilestoneController(PlanningMilestoneService service) { this.service = service; }
    @PostMapping public Mono<PlanningMilestoneView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @Valid @RequestBody CreatePlanningMilestoneRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningMilestoneView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @RequestParam(required = false) UUID goalId) { return service.list(ownerId, goalId); }
    @PatchMapping("/{milestoneId}") public Mono<PlanningMilestoneView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID milestoneId, @Valid @RequestBody UpdatePlanningMilestoneRequest request) { return service.update(ownerId, milestoneId, request); }
    @PostMapping("/{milestoneId}/status/{status}") public Mono<PlanningMilestoneView> status(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID milestoneId, @PathVariable PlanningMilestoneStatus status) { return service.changeStatus(ownerId, milestoneId, status); }
}
