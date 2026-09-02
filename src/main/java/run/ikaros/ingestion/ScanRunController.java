package run.ikaros.ingestion;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/ingestion/sources", "/api/v2/ingestion/sources"})
public class ScanRunController {
    private final ScanRunService service;
    public ScanRunController(ScanRunService service) { this.service = service; }

    @PostMapping("/{sourceId}/scans")
    public Mono<ScanRunView> start(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId, @PathVariable UUID sourceId,
                                   @Valid @RequestBody StartScanRequest request) {
        return service.start(actorId, sourceId, request);
    }

    @GetMapping("/scans")
    public Mono<List<ScanRunView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) { return service.list(actorId); }

    @GetMapping("/scans/{scanId}")
    public Mono<ScanRunView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId, @PathVariable UUID scanId) {
        return service.get(actorId, scanId);
    }

    @DeleteMapping("/scans/{scanId}")
    public Mono<ScanRunView> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId, @PathVariable UUID scanId) {
        return service.cancel(actorId, scanId);
    }
}
