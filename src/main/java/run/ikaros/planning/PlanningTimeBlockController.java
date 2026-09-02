package run.ikaros.planning;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/v2/planning/time-blocks")
public class PlanningTimeBlockController {
    private final PlanningTimeBlockService service;
    public PlanningTimeBlockController(PlanningTimeBlockService service) { this.service = service; }
    @PostMapping public Mono<PlanningTimeBlockView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody CreatePlanningTimeBlockRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningTimeBlockView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) { return service.list(ownerId, from, to); }
    @PatchMapping("/{blockId}") public Mono<ResponseEntity<PlanningTimeBlockView>> update(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID blockId, @RequestHeader(value="If-Match",required=false) String ifMatch,
        @Valid @RequestBody UpdatePlanningTimeBlockRequest request) { long version=IfMatchVersion.parse(ifMatch); return service.update(ownerId, blockId,
        new UpdatePlanningTimeBlockRequest(request.startAt(),request.endAt(),request.kind(),request.timeZone(),version))
        .map(view->ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
    @DeleteMapping("/{blockId}") public Mono<ResponseEntity<PlanningTimeBlockView>> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID blockId, @RequestHeader(value="If-Match",required=false) String ifMatch) { return service.cancel(ownerId, blockId, IfMatchVersion.parse(ifMatch)).map(view->ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
}
