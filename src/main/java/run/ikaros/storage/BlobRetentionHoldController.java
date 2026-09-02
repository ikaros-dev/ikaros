package run.ikaros.storage;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v2/storage/blobs/{blobId}/retention-holds")
public class BlobRetentionHoldController {
    private final BlobRetentionHoldService service;
    public BlobRetentionHoldController(BlobRetentionHoldService service) { this.service = service; }
    @PostMapping
    public Mono<BlobRetentionHoldView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID blobId, @Valid @RequestBody BlobRetentionHoldRequest request) { return service.create(actorId, blobId, request); }
    @GetMapping
    public Flux<BlobRetentionHoldView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID blobId) { return service.list(actorId, blobId); }
    @DeleteMapping("/{holdId}")
    public Mono<Void> release(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID holdId) { return service.release(actorId, holdId); }
}
