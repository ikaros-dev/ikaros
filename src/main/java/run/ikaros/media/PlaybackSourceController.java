package run.ikaros.media;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/media/resources/{resourceId}/playback-source")
public class PlaybackSourceController {
    private final PlaybackSourceService service;
    public PlaybackSourceController(PlaybackSourceService service) { this.service = service; }
    @GetMapping public Mono<PlaybackSourceView> resolve(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,
        @PathVariable UUID resourceId, @RequestParam(required = false) UUID releaseId) {
        return service.resolve(owner, resourceId, releaseId);
    }
}
