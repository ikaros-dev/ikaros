package run.ikaros.storage;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.ResponseEntity;

/** OpenAPI-compatible aliases for restore request routes. */
@RestController
public class StorageRestoreContractController {
    private final StorageRestoreRequestService service;
    public StorageRestoreContractController(StorageRestoreRequestService service) { this.service = service; }

    @PostMapping("/api/v2/attachments/{attachmentId}/restore-requests")
    public Mono<ResponseEntity<RestoreRequestContractView>> requestAttachment(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, @PathVariable UUID attachmentId,
        @RequestBody(required=false) RequestAttachmentRestore options) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        return service.requestAttachment(actorId, new RequestAttachmentRestore(attachmentId,
            options == null ? null : options.providerRestoreClass()), idempotencyKey)
            .map(view -> ResponseEntity.accepted().header("Location", "/api/v2/restore-requests/" + view.id()).body(contract(view)));
    }

    @GetMapping("/api/v2/restore-requests/{requestId}")
    public Mono<RestoreRequestContractView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID requestId) { return service.get(actorId, requestId).map(this::contract); }

    @GetMapping("/api/v2/restore-requests")
    public Mono<RestoreRequestContractListView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestParam(required=false) String cursor, @RequestParam(required=false) String status) {
        return service.list(actorId).map(this::contract).collectList().map(items -> new RestoreRequestContractListView(items, null));
    }

    @DeleteMapping("/api/v2/restore-requests/{requestId}")
    public Mono<ResponseEntity<RestoreRequestContractView>> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID requestId) {
        return service.cancel(actorId, requestId)
            .map(view -> ResponseEntity.accepted().body(contract(view)));
    }

    private RestoreRequestContractView contract(StorageRestoreRequestView view) {
        int failed = view.status() == StorageRestoreRequestStatus.FAILED || view.status() == StorageRestoreRequestStatus.PARTIAL_FAILURE
            ? Math.max(0, view.totalItems() - view.completedItems()) : 0;
        return new RestoreRequestContractView(view.id(), view.scope().name(), view.scopeId(), status(view.status()), view.totalItems(),
            view.totalBytes(), view.completedItems(), failed, "ACCEPTED", view.createdAt());
    }

    private String status(StorageRestoreRequestStatus value) {
        return switch (value) {
            case REQUESTED -> "PENDING";
            case IN_PROGRESS -> "ACTIVE";
            case COMPLETED -> "SUCCEEDED";
            case PARTIAL_FAILURE -> "PARTIAL";
            case FAILED -> "FAILED";
            case CANCELLED -> "CANCELLED";
        };
    }
}
