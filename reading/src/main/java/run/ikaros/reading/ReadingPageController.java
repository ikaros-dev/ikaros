package run.ikaros.reading;

import java.util.UUID;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/reading")
public class ReadingPageController {
    private final ReadingPageService service;

    public ReadingPageController(ReadingPageService service) { this.service = service; }

    @GetMapping("/chapters/{chapterId}/pages")
    public Flux<ComicPageView> pages(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID chapterId) { return service.list(ownerId, chapterId); }

    @GetMapping("/pages/{pageId}/content")
    public ResponseEntity<Flux<DataBuffer>> content(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID pageId) { return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(service.content(ownerId, pageId)); }
}
