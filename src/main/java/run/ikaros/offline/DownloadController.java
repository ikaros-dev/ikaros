package run.ikaros.offline;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping("/api/offline/downloads")
public class DownloadController {
    private final DownloadService service;
    public DownloadController(DownloadService service, DownloadManifestService manifests){this.service=service;this.manifests=manifests;}
    @PostMapping public Mono<DownloadView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@Valid @RequestBody CreateDownloadRequest request){return service.create(user,request);}
    @GetMapping public Flux<DownloadView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@RequestParam UUID deviceId){return service.list(user,deviceId);}
    @PatchMapping("/{intentId}") public Mono<DownloadView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@PathVariable UUID intentId,@Valid @RequestBody UpdateDownloadStateRequest request){return service.updateState(user,intentId,request);}
    @DeleteMapping("/{intentId}") public Mono<DownloadView> remove(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@PathVariable UUID intentId){return service.remove(user,intentId);}
    @PostMapping("/{intentId}/manifest") public Mono<DownloadManifestView> createManifest(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@PathVariable UUID intentId,@Valid @RequestBody CreateDownloadManifestRequest request){return manifests.create(user,intentId,request);}
    @GetMapping("/{intentId}/manifest") public Mono<DownloadManifestView> getManifest(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@PathVariable UUID intentId){return manifests.get(user,intentId);}

    private final DownloadManifestService manifests;
}
