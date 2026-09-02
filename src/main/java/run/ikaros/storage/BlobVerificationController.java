package run.ikaros.storage;

import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/storage/blobs")
public class BlobVerificationController {
    private final BlobVerificationService service;
    public BlobVerificationController(BlobVerificationService service) { this.service = service; }
    @PostMapping("/{blobId}/actions/verify")
    public Mono<BlobVerificationView> verify(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID blobId) { return service.verify(actorId, blobId); }
}
