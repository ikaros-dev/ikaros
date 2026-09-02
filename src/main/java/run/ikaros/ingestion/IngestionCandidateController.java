package run.ikaros.ingestion;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/ingestion/scans", "/api/v2/ingestion/scans"})
public class IngestionCandidateController {
    private final IngestionCandidateService service;
    public IngestionCandidateController(IngestionCandidateService service) { this.service = service; }
    @PostMapping("/{scanId}/candidates")
    public Mono<IngestionCandidateView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID scanId, @Valid @RequestBody CreateCandidateRequest request) {
        return service.create(actorId, scanId, request);
    }
    @GetMapping("/{scanId}/candidates")
    public Mono<List<IngestionCandidateView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID scanId) { return service.list(actorId, scanId); }
}
