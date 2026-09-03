package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/planning/focus-sessions")
public class PlanningFocusSessionController {
    private final PlanningFocusSessionService service;
    public PlanningFocusSessionController(PlanningFocusSessionService service) { this.service = service; }
    @PostMapping public Mono<PlanningFocusSessionView> start(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody StartPlanningFocusSessionRequest request) { return service.start(ownerId, request); }
    @GetMapping public Flux<PlanningFocusSessionView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @PostMapping("/{sessionId}/complete") public Mono<ResponseEntity<PlanningFocusSessionView>> complete(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID sessionId, @RequestHeader(value="If-Match", required=false) String ifMatch, @Valid @RequestBody CompletePlanningFocusSessionRequest request) { return service.complete(ownerId, sessionId, request, IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
    @DeleteMapping("/{sessionId}") public Mono<ResponseEntity<PlanningFocusSessionView>> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID sessionId, @RequestHeader(value="If-Match", required=false) String ifMatch) { return service.cancel(ownerId, sessionId, IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
}
