package run.ikaros.planning;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning/reminders")
public class PlanningReminderController {
    private final PlanningReminderService service;
    public PlanningReminderController(PlanningReminderService service) { this.service = service; }
    @PostMapping public Mono<PlanningReminderView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody CreatePlanningReminderRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningReminderView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @PostMapping("/{reminderId}/acknowledge") public Mono<PlanningReminderView> acknowledge(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID reminderId) { return service.acknowledge(ownerId, reminderId); }
    @PostMapping("/{reminderId}/snooze") public Mono<PlanningReminderView> snooze(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID reminderId, @RequestParam Instant until) { return service.snooze(ownerId, reminderId, until); }
    @DeleteMapping("/{reminderId}") public Mono<PlanningReminderView> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID reminderId) { return service.cancel(ownerId, reminderId); }
}
