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
            options == null ? null : options.providerRestoreClass(),
            options == null ? null : options.budgetConfirmationToken()), idempotencyKey)
            .map(view -> ResponseEntity.accepted().header("Location", "/api/v2/restore-requests/" + view.id()).body(contract(view)));
    }

    @GetMapping("/api/v2/restore-requests/{requestId}")
    public Mono<RestoreRequestContractView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID requestId) { return service.get(actorId, requestId).map(this::contract); }

    @GetMapping("/api/v2/restore-requests")
    public Mono<RestoreRequestContractListView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestParam(required=false) String cursor, @RequestParam(required=false) String status) {
        return service.listPage(actorId, internalStatus(status), cursor)
            .map(page -> new RestoreRequestContractListView(page.items().stream().map(this::contract).toList(), page.nextCursor()));
    }

    @DeleteMapping("/api/v2/restore-requests/{requestId}")
    public Mono<ResponseEntity<RestoreRequestContractView>> cancel(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID requestId) {
        return service.cancel(actorId, requestId)
            .map(view -> ResponseEntity.accepted().body(contract(view)));
    }

    @PostMapping("/api/v2/restore-requests/{requestId}/actions/retry")
    public Mono<ResponseEntity<RestoreRequestContractView>> retry(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, @PathVariable UUID requestId) {
        return service.retry(actorId, requestId, idempotencyKey)
            .map(view -> ResponseEntity.accepted().body(contract(view)));
    }

    private RestoreRequestContractView contract(StorageRestoreRequestView view) {
        int failed = view.status() == StorageRestoreRequestStatus.FAILED || view.status() == StorageRestoreRequestStatus.PARTIAL_FAILURE
            ? Math.max(0, view.totalItems() - view.completedItems()) : 0;
        return new RestoreRequestContractView(view.id(), view.scope().name(), view.scopeId(), status(view.status()), view.totalItems(),
            view.totalBytes(), view.completedItems(), failed, view.budgetDecision(), view.createdAt());
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

    private StorageRestoreRequestStatus internalStatus(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.toUpperCase()) {
            case "PENDING" -> StorageRestoreRequestStatus.REQUESTED;
            case "ACTIVE" -> StorageRestoreRequestStatus.IN_PROGRESS;
            case "PARTIAL" -> StorageRestoreRequestStatus.PARTIAL_FAILURE;
            case "SUCCEEDED" -> StorageRestoreRequestStatus.COMPLETED;
            case "FAILED" -> StorageRestoreRequestStatus.FAILED;
            case "CANCELLED" -> StorageRestoreRequestStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Restore Request status 无效");
        };
    }
}
