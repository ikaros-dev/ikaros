package run.ikaros.storage;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/storage/restore-requests")
public class StorageRestoreRequestController {
    private final StorageRestoreRequestService service;
    public StorageRestoreRequestController(StorageRestoreRequestService service) { this.service = service; }

    @PostMapping("/attachments")
    public Mono<StorageRestoreRequestView> request(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody RequestAttachmentRestore request) {
        return service.requestAttachment(actorId, request, idempotencyKey);
    }

    @GetMapping("/{id}")
    public Mono<StorageRestoreRequestView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID id) { return service.get(actorId, id); }

    @GetMapping
    public Flux<StorageRestoreRequestView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId) {
        return service.list(actorId);
    }
}
