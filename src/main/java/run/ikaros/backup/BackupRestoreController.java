package run.ikaros.backup;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping("/api/v2/admin/backup")
public class BackupRestoreController {
    private final BackupRestoreService service;
    public BackupRestoreController(BackupRestoreService service){this.service=service;}
    @PostMapping("/restore-points") public Mono<RestorePointView> create(@Valid @RequestBody CreateRestorePointRequest request){return service.create(request);}
    @GetMapping("/restore-points") public Flux<RestorePointView> list(){return service.list();}
    @PostMapping("/restore-points/{id}/verify") public Mono<RestorePointView> verify(@PathVariable UUID id,@Valid @RequestBody VerifyRestorePointRequest request){return service.verify(id,request);}
    @PostMapping("/restore-points/{id}/publish") public Mono<RestorePointView> publish(@PathVariable UUID id){return service.publish(id);}
}
