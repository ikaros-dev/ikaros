package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import run.ikaros.common.IfMatchVersion;

@RestController
@RequestMapping("/api/v2/planning/habits")
public class PlanningHabitController {
    private final PlanningHabitService service;
    public PlanningHabitController(PlanningHabitService service) { this.service = service; }
    @PostMapping public Mono<PlanningHabitView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @Valid @RequestBody CreatePlanningHabitRequest request) { return service.create(ownerId, request); }
    @GetMapping public Flux<PlanningHabitView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @DeleteMapping("/{habitId}") public Mono<ResponseEntity<PlanningHabitView>> archive(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID habitId, @RequestHeader(value="If-Match", required=false) String ifMatch) { return service.archive(ownerId, habitId, IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
    @PostMapping("/{habitId}/check-ins") public Mono<PlanningHabitCheckInView> checkIn(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID habitId, @Valid @RequestBody CreatePlanningHabitCheckInRequest request) { return service.checkIn(ownerId, habitId, request); }
    @GetMapping("/{habitId}/check-ins") public Flux<PlanningHabitCheckInView> listCheckIns(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID habitId) { return service.listCheckIns(ownerId, habitId); }
}
