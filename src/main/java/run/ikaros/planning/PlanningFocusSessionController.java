package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/focus-sessions")
public class PlanningFocusSessionController {
    private final PlanningFocusSessionService service;
    public PlanningFocusSessionController(PlanningFocusSessionService service) { this.service = service; }
    @PostMapping public Mono<PlanningFocusSessionView> start(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody StartPlanningFocusSessionRequest request) { return service.start(ownerId, request); }
    @GetMapping public Flux<PlanningFocusSessionView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @PostMapping("/{sessionId}/complete") public Mono<PlanningFocusSessionView> complete(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID sessionId, @Valid @RequestBody CompletePlanningFocusSessionRequest request) { return service.complete(ownerId, sessionId, request); }
    @DeleteMapping("/{sessionId}") public Mono<PlanningFocusSessionView> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID sessionId) { return service.cancel(ownerId, sessionId); }
}
