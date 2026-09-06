package run.ikaros.planning;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;
import run.ikaros.common.IfMatchVersion;
@RestController
@RequestMapping("/api/planning/tasks")
public class PlanningTaskController {
    private final PlanningTaskService service;
    public PlanningTaskController(PlanningTaskService service){this.service=service;}
    @PostMapping public Mono<PlanningTaskView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@Valid @RequestBody CreatePlanningTaskRequest request){return service.create(owner,request);}
    @GetMapping public Flux<PlanningTaskView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@RequestParam(required=false) PlanningTaskStatus status){return service.list(owner,status);}
    @GetMapping("/today") public Flux<PlanningTaskView> today(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@RequestParam(defaultValue="UTC") String timeZone){return service.today(owner,ZoneId.of(timeZone));}
    @GetMapping("/upcoming") public Flux<PlanningTaskView> upcoming(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@RequestParam(required=false) Instant from){return service.upcoming(owner,from);}
    @GetMapping("/smart") public Flux<PlanningTaskView> smart(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@RequestParam(required=false) PlanningTaskStatus status,@RequestParam(required=false) PlanningTaskPriority priority,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,@RequestParam(defaultValue="false") boolean overdue){return service.filter(owner,status,priority,from,to,overdue);}
    @GetMapping("/eisenhower") public Flux<PlanningTaskView> eisenhower(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@RequestParam boolean important,@RequestParam boolean urgent){return service.eisenhower(owner,important,urgent);}
    @PatchMapping("/{taskId}") public Mono<ResponseEntity<PlanningTaskView>> update(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID taskId,@RequestHeader(value="If-Match",required=false) String ifMatch,@Valid @RequestBody UpdatePlanningTaskRequest request){long version=IfMatchVersion.parse(ifMatch);return service.update(owner,taskId,new UpdatePlanningTaskRequest(request.title(),request.description(),request.priority(),request.important(),request.urgent(),request.scheduledStart(),request.scheduledEnd(),request.deadline(),request.estimatedDurationMinutes(),request.sectionId(),version)).map(view->ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));}
    @PostMapping("/{taskId}/status/{status}") public Mono<ResponseEntity<PlanningTaskView>> status(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID taskId,@PathVariable PlanningTaskStatus status,@RequestHeader(value="If-Match",required=false) String ifMatch){return service.changeStatus(owner,taskId,status,IfMatchVersion.parse(ifMatch)).map(view->ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));}
}
