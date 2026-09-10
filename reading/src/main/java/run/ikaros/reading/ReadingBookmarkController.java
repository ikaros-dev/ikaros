package run.ikaros.reading;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reading")
public class ReadingBookmarkController {
    private final ReadingBookmarkService service;

    public ReadingBookmarkController(ReadingBookmarkService service) { this.service = service; }

    @GetMapping("/works/{workId}/bookmarks")
    public Flux<ReadingBookmarkView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
                                          @PathVariable UUID workId) { return service.list(ownerId, workId); }

    @PostMapping("/works/{workId}/editions/{editionId}/chapters/{chapterId}/bookmarks")
    public Mono<ReadingBookmarkView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
                                            @PathVariable UUID workId, @PathVariable UUID editionId,
                                            @PathVariable UUID chapterId,
                                            @Valid @RequestBody CreateReadingBookmarkRequest request) {
        return service.create(ownerId, workId, editionId, chapterId, request);
    }

    @DeleteMapping("/bookmarks/{bookmarkId}")
    public Mono<Void> delete(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
                             @PathVariable UUID bookmarkId) { return service.delete(ownerId, bookmarkId); }
}
