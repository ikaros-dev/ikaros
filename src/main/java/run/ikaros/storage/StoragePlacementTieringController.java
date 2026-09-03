package run.ikaros.storage;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.ikaros.task.BackgroundTask;

@RestController
@RequestMapping("/api/v2/storage/placements")
public class StoragePlacementTieringController {
    private final StoragePlacementTieringService service;

    public StoragePlacementTieringController(StoragePlacementTieringService service) { this.service = service; }

    @PostMapping("/{placementId}/actions/promote")
    public Mono<ResponseEntity<BackgroundTask>> promote(@PathVariable UUID placementId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody StoragePlacementTieringRequest request) {
        return service.promote(placementId, request.targetTier(), idempotencyKey).map(this::accepted);
    }

    @PostMapping("/{placementId}/actions/demote")
    public Mono<ResponseEntity<BackgroundTask>> demote(@PathVariable UUID placementId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody StoragePlacementTieringRequest request) {
        return service.demote(placementId, request.targetTier(), idempotencyKey).map(this::accepted);
    }

    private ResponseEntity<BackgroundTask> accepted(BackgroundTask task) {
        return ResponseEntity.accepted().location(URI.create("/api/v2/background-tasks/" + task.id())).body(task);
    }
}
