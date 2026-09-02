package run.ikaros.ingestion;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping({"/api/ingestion/conflicts","/api/v2/ingestion/conflicts"})
public class ImportConflictController {
    private final ImportConflictService service;
    public ImportConflictController(ImportConflictService service){this.service=service;}
    @PostMapping public Mono<ImportConflictView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@Valid @RequestBody CreateImportConflictRequest request){return service.create(actor,request);}
    @GetMapping public Mono<List<ImportConflictView>> pending(@RequestHeader("X-Ikaros-Actor-Id") UUID actor){return service.pending(actor);}
    @PostMapping("/{conflictId}/resolve") public Mono<ImportConflictView> resolve(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID conflictId,@Valid @RequestBody ResolveImportConflictRequest request){return service.resolve(actor,conflictId,request);}
}
