package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/projects")
public class PlanningProjectController {
    private final PlanningProjectService service;
    public PlanningProjectController(PlanningProjectService service) { this.service = service; }

    @PostMapping public Mono<PlanningProjectView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody CreatePlanningProjectRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningProjectView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @PatchMapping("/{projectId}") public Mono<PlanningProjectView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID projectId, @Valid @RequestBody UpdatePlanningProjectRequest request) { return service.update(ownerId, projectId, request); }
    @PostMapping("/{projectId}/status/{status}") public Mono<PlanningProjectView> status(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID projectId, @PathVariable PlanningProjectStatus status) { return service.changeStatus(ownerId, projectId, status); }
}
