package run.ikaros.ingestion;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping({"/api/ingestion"})
public class ImportRunController {
    private final ImportRunService service;
    public ImportRunController(ImportRunService service){this.service=service;}
    @PostMapping("/plans/{planId}/runs") public Mono<ImportRunView> start(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,
        @PathVariable UUID planId,@Valid @RequestBody StartImportRequest request){return service.start(actor,planId,request);}
    @GetMapping("/runs") public Mono<List<ImportRunView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actor){return service.list(actor);}
    @GetMapping("/runs/{runId}") public Mono<ImportRunView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID runId){return service.get(actor,runId);}
    @DeleteMapping("/runs/{runId}") public Mono<ImportRunView> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID runId){return service.cancel(actor,runId);}
    @GetMapping("/runs/{runId}/items") public Mono<List<ImportRunItemView>> items(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID runId){return service.items(actor,runId);}
    @PostMapping("/runs/{runId}/items/{itemId}/retry") public Mono<ImportRunItemView> retry(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID runId,@PathVariable UUID itemId){return service.retryItem(actor,runId,itemId);}
}
