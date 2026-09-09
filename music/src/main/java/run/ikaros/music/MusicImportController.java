package run.ikaros.music;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/music/imports")
public class MusicImportController {
    private final MusicImportService service;
    public MusicImportController(MusicImportService service) { this.service = service; }
    @PostMapping public Mono<MusicImportView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CreateMusicImportRequest request) { return service.create(ownerId, request, key); }
    @GetMapping public Flux<MusicImportView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId) { return service.list(ownerId); }
}
