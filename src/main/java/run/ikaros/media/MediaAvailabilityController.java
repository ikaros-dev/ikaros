package run.ikaros.media;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/media/resources/{resourceId}/availability")
public class MediaAvailabilityController {
    private final MediaAvailabilityService service;

    public MediaAvailabilityController(MediaAvailabilityService service) { this.service = service; }

    @GetMapping
    public Mono<MediaAvailabilityView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,
        @PathVariable UUID resourceId) { return service.get(owner, resourceId); }
}
