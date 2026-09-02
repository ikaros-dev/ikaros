package run.ikaros.media;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.progress.ResourceProgressView;

@RestController
@RequestMapping("/api/v2/media/playback")
public class MediaPlaybackController {
    private final MediaPlaybackService service;
    public MediaPlaybackController(MediaPlaybackService service) { this.service = service; }
    @PostMapping("/resources/{resourceId}/sessions") public Mono<PlaybackSessionView> start(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID resourceId, @Valid @RequestBody StartPlaybackRequest request) { return service.start(owner, resourceId, request); }
    @PatchMapping("/sessions/{sessionId}") public Mono<PlaybackSessionView> update(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID sessionId, @Valid @RequestBody UpdatePlaybackProgressRequest request) { return service.update(owner, sessionId, request); }
    @PostMapping("/sessions/{sessionId}/actions/end") public Mono<PlaybackSessionView> end(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID sessionId) { return service.end(owner, sessionId); }
    @GetMapping("/resources/{resourceId}/progress") public Mono<ResourceProgressView> progress(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID resourceId) { return service.progress(owner, resourceId); }
    @GetMapping("/history") public Flux<PlaybackHistoryView> history(@RequestHeader("X-Ikaros-Actor-Id") UUID owner) { return service.history(owner); }
}
