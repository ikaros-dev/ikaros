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
@RequestMapping("/api/planning/reminders")
public class PlanningReminderController {
    private final PlanningReminderService service;
    public PlanningReminderController(PlanningReminderService service) { this.service = service; }
    @PostMapping public Mono<PlanningReminderView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody CreatePlanningReminderRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningReminderView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @PostMapping("/{reminderId}/acknowledge") public Mono<ResponseEntity<PlanningReminderView>> acknowledge(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID reminderId, @RequestHeader(value="If-Match", required=false) String ifMatch) { return service.acknowledge(ownerId, reminderId, IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
    @PostMapping("/{reminderId}/snooze") public Mono<ResponseEntity<PlanningReminderView>> snooze(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID reminderId, @RequestParam Instant until, @RequestHeader(value="If-Match", required=false) String ifMatch) { return service.snooze(ownerId, reminderId, until, IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
    @DeleteMapping("/{reminderId}") public Mono<ResponseEntity<PlanningReminderView>> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID reminderId, @RequestHeader(value="If-Match", required=false) String ifMatch) { return service.cancel(ownerId, reminderId, IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
}
