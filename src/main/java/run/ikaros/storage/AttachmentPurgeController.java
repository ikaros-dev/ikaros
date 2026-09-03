package run.ikaros.storage;

import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/resources/{resourceId}/attachments")
public class AttachmentPurgeController {
    private final AttachmentPurgeService service;
    public AttachmentPurgeController(AttachmentPurgeService service) { this.service = service; }
    @PostMapping("/{attachmentId}/actions/purge")
    public Mono<Void> purge(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId, @PathVariable UUID attachmentId) {
        return service.purge(actorId, resourceId, attachmentId);
    }
}
