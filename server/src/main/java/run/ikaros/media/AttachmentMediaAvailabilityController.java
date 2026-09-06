package run.ikaros.media;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Attachment-shaped alias required by the media delivery contract. */
@RestController
@RequestMapping("/api/attachments/{attachmentId}/availability")
public class AttachmentMediaAvailabilityController {
    private final AttachmentMediaAvailabilityService service;
    public AttachmentMediaAvailabilityController(AttachmentMediaAvailabilityService service) { this.service = service; }
    @GetMapping
    public Mono<MediaAvailabilityResponse> get(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,
        @PathVariable UUID attachmentId) { return service.get(owner, attachmentId); }
}
