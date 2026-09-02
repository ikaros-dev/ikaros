package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/v2/planning/milestones")
public class PlanningMilestoneController {
    private final PlanningMilestoneService service;
    public PlanningMilestoneController(PlanningMilestoneService service) { this.service = service; }
    @PostMapping public Mono<PlanningMilestoneView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @Valid @RequestBody CreatePlanningMilestoneRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningMilestoneView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @RequestParam(required = false) UUID goalId) { return service.list(ownerId, goalId); }
    @PatchMapping("/{milestoneId}") public Mono<ResponseEntity<PlanningMilestoneView>> update(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID milestoneId, @RequestHeader(value="If-Match",required=false) String ifMatch, @Valid @RequestBody UpdatePlanningMilestoneRequest request) { long version=IfMatchVersion.parse(ifMatch); return service.update(ownerId, milestoneId, new UpdatePlanningMilestoneRequest(request.title(),request.description(),request.dueAt(),version)).map(view->ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
    @PostMapping("/{milestoneId}/status/{status}") public Mono<PlanningMilestoneView> status(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID milestoneId, @PathVariable PlanningMilestoneStatus status) { return service.changeStatus(ownerId, milestoneId, status); }
}
