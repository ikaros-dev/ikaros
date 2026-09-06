package run.ikaros.media;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/media")
public class MediaCatalogController {
    private final MediaCatalogService service;
    public MediaCatalogController(MediaCatalogService service) { this.service = service; }
    @PostMapping("/subjects") public Mono<MediaSubjectView> createSubject(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @Valid @RequestBody CreateMediaSubjectRequest request) { return service.createSubject(owner, request); }
    @GetMapping("/subjects") public Flux<MediaSubjectView> subjects(@RequestHeader("X-Ikaros-Actor-Id") UUID owner) { return service.listSubjects(owner); }
    @PostMapping("/subjects/{subjectId}/seasons") public Mono<MediaSeasonView> createSeason(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID subjectId, @Valid @RequestBody CreateMediaSeasonRequest request) { return service.createSeason(owner, subjectId, request); }
    @GetMapping("/subjects/{subjectId}/seasons") public Flux<MediaSeasonView> seasons(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID subjectId) { return service.listSeasons(owner, subjectId); }
    @PostMapping("/subjects/{subjectId}/episodes") public Mono<MediaEpisodeView> createEpisode(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID subjectId, @RequestParam UUID seasonId, @Valid @RequestBody CreateMediaEpisodeRequest request) { return service.createEpisode(owner, subjectId, seasonId, request); }
    @GetMapping("/subjects/{subjectId}/episodes") public Flux<MediaEpisodeView> episodes(@RequestHeader("X-Ikaros-Actor-Id") UUID owner, @PathVariable UUID subjectId, @RequestParam(required = false) UUID seasonId) { return service.listEpisodes(owner, subjectId, seasonId); }
}
