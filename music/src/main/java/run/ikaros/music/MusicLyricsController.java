package run.ikaros.music;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/music/tracks/{trackId}/lyrics")
public class MusicLyricsController {
    private final MusicLyricsService service;

    public MusicLyricsController(MusicLyricsService service) { this.service = service; }

    @GetMapping
    public Flux<MusicLyricsView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @PathVariable UUID trackId) { return service.list(ownerId, trackId); }
}
