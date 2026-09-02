package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/reviews")
public class PlanningReviewController {
    private final PlanningReviewService service;
    public PlanningReviewController(PlanningReviewService service) { this.service = service; }
    @PostMapping public Mono<PlanningReviewView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @Valid @RequestBody CreatePlanningReviewRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningReviewView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @RequestParam(required = false) PlanningReviewPeriod period) { return service.list(ownerId, period); }
    @PatchMapping("/{reviewId}") public Mono<PlanningReviewView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID reviewId, @Valid @RequestBody UpdatePlanningReviewRequest request) { return service.update(ownerId, reviewId, request); }
}
