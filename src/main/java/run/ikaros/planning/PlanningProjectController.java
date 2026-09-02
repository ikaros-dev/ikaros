package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/v2/planning/projects")
public class PlanningProjectController {
    private final PlanningProjectService service;
    public PlanningProjectController(PlanningProjectService service) { this.service = service; }

    @PostMapping public Mono<PlanningProjectView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody CreatePlanningProjectRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningProjectView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @PatchMapping("/{projectId}") public Mono<ResponseEntity<PlanningProjectView>> update(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID projectId, @RequestHeader(value="If-Match", required=false) String ifMatch,
        @Valid @RequestBody UpdatePlanningProjectRequest request) { long version=IfMatchVersion.parse(ifMatch); return service.update(ownerId, projectId,
        new UpdatePlanningProjectRequest(request.name(), request.description(), version)).map(view -> ResponseEntity.ok()
        .eTag(IfMatchVersion.etag(view.version())).body(view)); }
    @PostMapping("/{projectId}/status/{status}") public Mono<ResponseEntity<PlanningProjectView>> status(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID projectId, @PathVariable PlanningProjectStatus status,
        @RequestHeader(value="If-Match", required=false) String ifMatch) { return service.changeStatus(ownerId, projectId, status,
        IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
}
