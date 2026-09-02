package run.ikaros.media;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/media/resources/{resourceId}/releases")
public class MediaReleaseController {
    private final MediaReleaseService service;
    public MediaReleaseController(MediaReleaseService service) { this.service = service; }
    @PostMapping public Mono<MediaReleaseView> add(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID resourceId, @Valid @RequestBody CreateMediaReleaseRequest request) { return service.add(owner, resourceId, request); }
    @GetMapping public Flux<MediaReleaseView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID resourceId) { return service.list(owner, resourceId); }
    @PostMapping("/{releaseId}/actions/state") public Mono<MediaReleaseView> state(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID releaseId, @Valid @RequestBody UpdateMediaReleaseStateRequest request) { return service.changeState(owner, releaseId, request); }
}
