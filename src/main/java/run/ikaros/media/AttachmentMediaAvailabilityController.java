package run.ikaros.media;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.AttachmentRepository;
import run.ikaros.storage.BlobAvailability;
import run.ikaros.storage.BlobPlacementRepository;
import run.ikaros.storage.BlobRepository;
import run.ikaros.storage.PlacementState;

/** Attachment-shaped alias required by the media delivery contract. */
@RestController
@RequestMapping("/api/v2/attachments/{attachmentId}/availability")
public class AttachmentMediaAvailabilityController {
    private final AttachmentRepository attachments;
    private final MediaAvailabilityService availability;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    public AttachmentMediaAvailabilityController(AttachmentRepository attachments, MediaAvailabilityService availability,
        BlobRepository blobs, BlobPlacementRepository placements) {
        this.attachments = attachments; this.availability = availability; this.blobs = blobs; this.placements = placements;
    }
    @GetMapping
    public Mono<MediaAvailabilityResponse> get(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,
        @PathVariable UUID attachmentId) {
        return attachments.findById(attachmentId).filter(a -> a.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
            .flatMap(attachment -> availability.get(owner, attachment.resourceId())
                .then(blobs.findById(attachment.blobId())
                    .map(blob -> new AttachmentContext(attachmentId, blob))
                    .switchIfEmpty(Mono.error(new NotFoundException("附件引用的 Blob 不存在"))))
                .flatMap(context -> response(context.attachmentId(), context.blob())));
    }

    private Mono<MediaAvailabilityResponse> response(UUID attachmentId, run.ikaros.storage.BlobEntity blob) {
        return switch (blob.availability()) {
            case RESTORING -> Mono.just(new MediaAvailabilityResponse(attachmentId, MediaContractAvailability.RESTORING,
                null, null, null, null));
            case PROCESSING -> Mono.just(new MediaAvailabilityResponse(attachmentId, MediaContractAvailability.RESTORING,
                null, null, null, null));
            case MISSING -> Mono.just(new MediaAvailabilityResponse(attachmentId, MediaContractAvailability.MISSING,
                null, null, null, null));
            case CORRUPTED -> Mono.just(new MediaAvailabilityResponse(attachmentId, MediaContractAvailability.UNAVAILABLE,
                null, null, null, null));
            case AVAILABLE, REMOTE -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                .any(placement -> placement.placementState() == PlacementState.ACTIVE)
                .map(readable -> new MediaAvailabilityResponse(attachmentId,
                    readable ? MediaContractAvailability.READY : MediaContractAvailability.MISSING,
                    null, null, null, null));
        };
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

    private record AttachmentContext(UUID attachmentId, run.ikaros.storage.BlobEntity blob) {}
}
