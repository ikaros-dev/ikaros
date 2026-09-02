package run.ikaros.media;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.AttachmentRepository;

/** Attachment-shaped alias required by the media delivery contract. */
@RestController
@RequestMapping("/api/v2/attachments/{attachmentId}/availability")
public class AttachmentMediaAvailabilityController {
    private final AttachmentRepository attachments;
    private final MediaAvailabilityService availability;
    public AttachmentMediaAvailabilityController(AttachmentRepository attachments, MediaAvailabilityService availability) {
        this.attachments = attachments; this.availability = availability;
    }
    @GetMapping
    public Mono<MediaAvailabilityResponse> get(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,
        @PathVariable UUID attachmentId) {
        return attachments.findById(attachmentId).filter(a -> a.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
            .flatMap(attachment -> availability.get(owner, attachment.resourceId())
                .map(view -> new MediaAvailabilityResponse(attachmentId, contractStatus(view.availability()),
                    null, null, null, null)));
    }

    private MediaContractAvailability contractStatus(MediaAvailability value) {
        return switch (value) {
            case AVAILABLE -> MediaContractAvailability.READY;
            case PROCESSING -> MediaContractAvailability.RESTORING;
            case RESTORE_REQUIRED -> MediaContractAvailability.RESTORE_REQUIRED;
            case CORRUPTED -> MediaContractAvailability.UNAVAILABLE;
            case MISSING -> MediaContractAvailability.MISSING;
        };
    }
}
