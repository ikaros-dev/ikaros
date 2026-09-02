package run.ikaros.storage;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/media/seasons/{seasonId}/restore-requests")
public class StorageRestoreSeasonController {
    private final StorageRestoreRequestService service;
    public StorageRestoreSeasonController(StorageRestoreRequestService service) { this.service = service; }
    @PostMapping
    public Mono<ResponseEntity<RestoreRequestContractView>> request(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, @PathVariable UUID seasonId,
        @RequestBody(required=false) RequestAttachmentRestore options) {
        return service.requestSeason(actorId, seasonId, options == null ? null : options.providerRestoreClass(), idempotencyKey)
            .map(view -> ResponseEntity.accepted().header("Location", "/api/v2/restore-requests/" + view.id()).body(
                new RestoreRequestContractView(view.id(), view.scope().name(), view.scopeId(), view.status(), view.totalItems(),
                    view.totalBytes(), view.completedItems(), view.status() == StorageRestoreRequestStatus.FAILED
                        || view.status() == StorageRestoreRequestStatus.PARTIAL_FAILURE ? view.totalItems() - view.completedItems() : 0,
                    "ACCEPTED", view.createdAt())));
    }
}
