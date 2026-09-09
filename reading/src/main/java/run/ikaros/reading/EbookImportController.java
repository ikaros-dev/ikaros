package run.ikaros.reading;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reading/ebook-imports")
public class EbookImportController {
    private final EbookImportService service;
    private final EbookBookInfoService bookInfoService;
    private final EbookChapterContentService chapterContentService;
    public EbookImportController(EbookImportService service, EbookBookInfoService bookInfoService,
                                 EbookChapterContentService chapterContentService) {
        this.service = service; this.bookInfoService = bookInfoService; this.chapterContentService = chapterContentService;
    }
    @PostMapping public Mono<EbookImportView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CreateEbookImportRequest request) { return service.create(ownerId, request, key); }
    @GetMapping public Flux<EbookImportView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
    @GetMapping("/{importId}") public Mono<EbookImportView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID importId) { return service.get(ownerId, importId); }
    @GetMapping("/{importId}/info") public Mono<EbookBookInfoView> info(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId, @PathVariable UUID importId) { return bookInfoService.get(ownerId, importId); }
    @GetMapping("/{importId}/chapters/{chapterId}/content")
    public Mono<EbookChapterContentView> content(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
                                                 @PathVariable UUID importId, @PathVariable UUID chapterId) {
        return chapterContentService.get(ownerId, importId, chapterId);
    }
}
