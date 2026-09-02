package run.ikaros.planning;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping("/api/v2/planning/tasks")
public class PlanningTaskController {
    private final PlanningTaskService service;
    public PlanningTaskController(PlanningTaskService service){this.service=service;}
    @PostMapping public Mono<PlanningTaskView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@Valid @RequestBody CreatePlanningTaskRequest request){return service.create(owner,request);}
    @GetMapping public Flux<PlanningTaskView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@RequestParam(required=false) PlanningTaskStatus status){return service.list(owner,status);}
    @PatchMapping("/{taskId}") public Mono<PlanningTaskView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID taskId,@Valid @RequestBody UpdatePlanningTaskRequest request){return service.update(owner,taskId,request);}
    @PostMapping("/{taskId}/status/{status}") public Mono<PlanningTaskView> status(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID taskId,@PathVariable PlanningTaskStatus status){return service.changeStatus(owner,taskId,status);}
}
