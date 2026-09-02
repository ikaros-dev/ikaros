package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v2/planning/calendar")
public class PlanningCalendarController {
    private final PlanningCalendarService service;
    public PlanningCalendarController(PlanningCalendarService service) { this.service = service; }
    @GetMapping public Flux<PlanningCalendarItemView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestParam Instant from, @RequestParam Instant to) { return service.list(ownerId, from, to); }
}
