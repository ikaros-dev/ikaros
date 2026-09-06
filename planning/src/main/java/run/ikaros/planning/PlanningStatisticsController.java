package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/planning/statistics")
public class PlanningStatisticsController {
    private final PlanningStatisticsService service;
    public PlanningStatisticsController(PlanningStatisticsService service) { this.service = service; }
    @GetMapping public Mono<PlanningStatisticsView> summarize(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestParam Instant from, @RequestParam Instant to) { return service.summarize(ownerId, from, to); }
}
