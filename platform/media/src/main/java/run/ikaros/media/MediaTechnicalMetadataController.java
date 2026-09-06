package run.ikaros.media;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/media/releases/{releaseId}")
public class MediaTechnicalMetadataController {
    private final MediaTechnicalMetadataService service;
    public MediaTechnicalMetadataController(MediaTechnicalMetadataService service) { this.service = service; }
    @PutMapping("/probe") public Mono<MediaProbeView> probe(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID releaseId, @Valid @RequestBody UpsertMediaProbeRequest request) { return service.upsertProbe(owner, releaseId, request); }
    @GetMapping("/probe") public Mono<MediaProbeView> getProbe(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID releaseId, @RequestParam String profileVersion) { return service.getProbe(owner, releaseId, profileVersion); }
    @PostMapping("/subtitles") public Mono<MediaExternalSubtitleView> subtitle(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID releaseId, @Valid @RequestBody AddExternalSubtitleRequest request) { return service.addSubtitle(owner, releaseId, request); }
    @GetMapping("/subtitles") public Flux<MediaExternalSubtitleView> subtitles(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID releaseId) { return service.listSubtitles(owner, releaseId); }
}
