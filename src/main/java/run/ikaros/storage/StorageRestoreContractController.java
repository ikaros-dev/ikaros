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
    public Mono<ResponseEntity<StorageRestoreRequestView>> requestAttachment(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, @PathVariable UUID attachmentId,
        @RequestBody(required=false) RequestAttachmentRestore options) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        return service.requestAttachment(actorId, new RequestAttachmentRestore(attachmentId,
            options == null ? null : options.providerRestoreClass()), idempotencyKey)
            .map(view -> ResponseEntity.accepted().header("Location", "/api/v2/restore-requests/" + view.id()).body(view));
    }

    @GetMapping("/api/v2/restore-requests/{requestId}")
    public Mono<StorageRestoreRequestView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID requestId) { return service.get(actorId, requestId); }

    @GetMapping("/api/v2/restore-requests")
    public Flux<StorageRestoreRequestView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestParam(required=false) String cursor, @RequestParam(required=false) String status) {
        return service.list(actorId);
    }
}
