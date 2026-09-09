package run.ikaros.reading;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reading/comic-imports")
public class ComicImportController {
    private final ComicImportService service;
    public ComicImportController(ComicImportService service) { this.service = service; }

    @PostMapping
    public Mono<ComicImportView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateComicImportRequest request) {
        return service.create(ownerId, request, idempotencyKey);
    }
    @GetMapping public Flux<ComicImportView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @GetMapping("/{importId}") public Mono<ComicImportView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID importId) { return service.get(ownerId, importId); }
}
