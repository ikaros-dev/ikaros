package run.ikaros.reading;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reading/ebook-imports")
public class EbookTocController {
    private final EbookTocParseService service;
    public EbookTocController(EbookTocParseService service) { this.service=service; }
    @PostMapping("/{importId}/actions/parse-toc") public Mono<EbookImportView> parse(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,@PathVariable UUID importId){return service.parse(ownerId,importId);}
    @GetMapping("/{importId}/chapters") public Flux<EbookChapterView> chapters(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,@PathVariable UUID importId){return service.chapters(ownerId,importId);}
}
