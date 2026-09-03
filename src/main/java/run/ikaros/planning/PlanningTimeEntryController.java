package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/planning/tasks/{taskId}/time-entries")
public class PlanningTimeEntryController {
    private final PlanningTimeEntryService service;
    public PlanningTimeEntryController(PlanningTimeEntryService service) { this.service = service; }
    @PostMapping public Mono<PlanningTimeEntryView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId, @Valid @RequestBody CreatePlanningTimeEntryRequest request) { return service.create(ownerId, taskId, request); }
    @GetMapping public Flux<PlanningTimeEntryView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId) { return service.list(ownerId, taskId); }
}
