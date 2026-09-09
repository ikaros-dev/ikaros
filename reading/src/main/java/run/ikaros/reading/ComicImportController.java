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
    private final ComicImportParseService parser;
    public ComicImportController(ComicImportService service, ComicImportParseService parser) { this.service = service; this.parser = parser; }

    @PostMapping
    public Mono<ComicImportView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateComicImportRequest request) {
        return service.create(ownerId, request, idempotencyKey);
    }
    @GetMapping public Flux<ComicImportView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @GetMapping("/{importId}") public Mono<ComicImportView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID importId) { return service.get(ownerId, importId); }
    @PostMapping("/{importId}/actions/parse") public Mono<ComicImportView> parse(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID importId) { return parser.parse(ownerId, importId); }
    @GetMapping("/{importId}/entries") public Flux<ComicImportEntryEntity> entries(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID importId) { return parser.entries(ownerId, importId); }
    @PostMapping("/{importId}/actions/reorder-pages") public Mono<ComicImportView> reorder(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID importId, @Valid @RequestBody ReorderComicPagesRequest request) { return parser.reorder(ownerId, importId, request); }
}
