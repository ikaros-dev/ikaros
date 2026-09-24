package run.ikaros.media;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.ikaros.storage.api.StorageRestoreSubmissionView;

@RestController
@RequestMapping("/api/media/seasons/{seasonId}/restore-requests")
public class MediaSeasonRestoreController {
    private final MediaSeasonRestoreService service;

    public MediaSeasonRestoreController(MediaSeasonRestoreService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<StorageRestoreSubmissionView>> request(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @PathVariable UUID seasonId,
        @RequestBody(required = false) MediaSeasonRestoreOptions options) {
        return service.requestSeason(actorId, seasonId, options, idempotencyKey)
            .map(view -> ResponseEntity.accepted()
                .header("Location", "/api/restore-requests/" + view.id())
                .body(view));
    }
}
