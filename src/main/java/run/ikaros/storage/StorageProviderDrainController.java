package run.ikaros.storage;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.ikaros.task.BackgroundTask;

@RestController
@RequestMapping("/api/storage/providers")
public class StorageProviderDrainController {
    private final StorageProviderDrainService service;
    public StorageProviderDrainController(StorageProviderDrainService service) { this.service = service; }
    @PostMapping("/{providerId}/actions/drain")
    public Mono<ResponseEntity<BackgroundTask>> drain(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @PathVariable UUID providerId) {
        return service.request(providerId, actorId, idempotencyKey).map(task -> ResponseEntity.accepted().header("Location",
            "/api/background-tasks/" + task.id()).body(task));
    }
}
