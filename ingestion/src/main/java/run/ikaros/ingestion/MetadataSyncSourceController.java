package run.ikaros.ingestion;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/metadata/sync-sources")
public class MetadataSyncSourceController {
    private final MetadataSyncSourceService service;
    private final MetadataSyncService syncService;

    public MetadataSyncSourceController(MetadataSyncSourceService service, MetadataSyncService syncService) {
        this.service = service;
        this.syncService = syncService;
    }

    @PostMapping
    public Mono<ResponseEntity<MetadataSyncSourceView>> create(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @Valid @RequestBody CreateMetadataSyncSourceRequest request) {
        return service.create(actorId, request).map(view -> ResponseEntity.created(
            URI.create("/api/metadata/sync-sources/" + view.id())).body(view));
    }

    @GetMapping
    public Mono<List<MetadataSyncSourceView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) {
        return service.list(actorId);
    }

    @PostMapping("/{sourceId}/enable")
    public Mono<MetadataSyncSourceView> enable(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                               @PathVariable UUID sourceId) {
        return service.enable(actorId, sourceId);
    }

    @PostMapping("/{sourceId}/refresh")
    public Mono<MetadataRefreshResult> refresh(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                               @PathVariable UUID sourceId,
                                               @Valid @RequestBody DetectMetadataUpdateRequest request) {
        return syncService.detect(actorId, sourceId, request);
    }

    @DeleteMapping("/{sourceId}")
    public Mono<ResponseEntity<Void>> disable(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                              @PathVariable UUID sourceId) {
        return service.disable(actorId, sourceId).thenReturn(ResponseEntity.noContent().build());
    }
}
