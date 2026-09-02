package run.ikaros.planning;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/planning")
public class PlanningTagController {
    private final PlanningTagService service;
    public PlanningTagController(PlanningTagService service) { this.service = service; }
    @PostMapping("/tags") public Mono<PlanningTagView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @Valid @RequestBody CreatePlanningTagRequest request) { return service.create(ownerId, request); }
    @GetMapping("/tags") public Flux<PlanningTagView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @GetMapping("/tasks/{taskId}/tags") public Flux<PlanningTagView> listForTask(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId) { return service.listForTask(ownerId, taskId); }
    @PutMapping("/tasks/{taskId}/tags/{tagId}") public Mono<Void> attach(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId, @PathVariable UUID tagId) { return service.attach(ownerId, taskId, tagId); }
    @DeleteMapping("/tasks/{taskId}/tags/{tagId}") public Mono<Void> detach(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID taskId, @PathVariable UUID tagId) { return service.detach(ownerId, taskId, tagId); }
}
