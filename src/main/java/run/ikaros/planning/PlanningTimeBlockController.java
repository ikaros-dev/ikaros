package run.ikaros.planning;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/time-blocks")
public class PlanningTimeBlockController {
    private final PlanningTimeBlockService service;
    public PlanningTimeBlockController(PlanningTimeBlockService service) { this.service = service; }
    @PostMapping public Mono<PlanningTimeBlockView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody CreatePlanningTimeBlockRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningTimeBlockView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) { return service.list(ownerId, from, to); }
    @PatchMapping("/{blockId}") public Mono<PlanningTimeBlockView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID blockId, @Valid @RequestBody UpdatePlanningTimeBlockRequest request) { return service.update(ownerId, blockId, request); }
    @DeleteMapping("/{blockId}") public Mono<PlanningTimeBlockView> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID blockId) { return service.cancel(ownerId, blockId); }
}
