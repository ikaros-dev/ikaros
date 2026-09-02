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
@RequestMapping({"/api/ingestion/sources", "/api/v2/ingestion/sources"})
public class IngestionSourceController {
    private final IngestionSourceService service;

    public IngestionSourceController(IngestionSourceService service) { this.service = service; }

    @PostMapping
    public Mono<ResponseEntity<IngestionSourceView>> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                                              @Valid @RequestBody CreateIngestionSourceRequest request) {
        return service.create(actorId, request).map(view -> ResponseEntity.created(
            URI.create("/api/v2/ingestion/sources/" + view.id())).body(view));
    }

    @GetMapping
    public Mono<List<IngestionSourceView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) {
        return service.list(actorId);
    }

    @GetMapping("/{sourceId}")
    public Mono<IngestionSourceView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                         @PathVariable UUID sourceId) {
        return service.get(actorId, sourceId);
    }

    @PostMapping("/{sourceId}/enable")
    public Mono<IngestionSourceView> enable(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                            @PathVariable UUID sourceId) {
        return service.enable(actorId, sourceId);
    }

    @DeleteMapping("/{sourceId}")
    public Mono<ResponseEntity<Void>> disable(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                              @PathVariable UUID sourceId) {
        return service.disable(actorId, sourceId).thenReturn(ResponseEntity.noContent().build());
    }
}
