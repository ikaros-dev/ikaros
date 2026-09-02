package run.ikaros.planning;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/tasks/{taskId}/recurrence")
public class PlanningRecurrenceController {
    private final PlanningRecurrenceService service;
    public PlanningRecurrenceController(PlanningRecurrenceService service) { this.service = service; }
    @PostMapping public Mono<PlanningRecurrenceView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId, @Valid @RequestBody CreatePlanningRecurrenceRequest request) { return service.create(ownerId, taskId, request); }
    @GetMapping public Mono<PlanningRecurrenceView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId) { return service.get(ownerId, taskId); }
    @PostMapping("/active/{active}") public Mono<PlanningRecurrenceView> active(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId, @PathVariable boolean active) { return service.setActive(ownerId, taskId, active); }
    @PostMapping("/skip") public Mono<PlanningRecurrenceView> skip(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId, @RequestParam(required = false) Instant nextRunAt) { return service.skip(ownerId, taskId, nextRunAt); }
}
