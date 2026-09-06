package run.ikaros.media;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.storage.api.AttachmentAvailabilityQuery;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentReferenceQuery;

@Service
public class AttachmentMediaAvailabilityService {
    private final AttachmentReferenceQuery references;
    private final AttachmentAvailabilityQuery attachmentAvailability;
    private final MediaAvailabilityService availability;

    public AttachmentMediaAvailabilityService(AttachmentReferenceQuery references,
        AttachmentAvailabilityQuery attachmentAvailability, MediaAvailabilityService availability) {
        this.references = references;
        this.attachmentAvailability = attachmentAvailability;
        this.availability = availability;
    }

    public Mono<MediaAvailabilityResponse> get(UUID owner, UUID attachmentId) {
        return references.requireReadable(owner, attachmentId)
            .flatMap(reference -> availability.get(owner, reference.resourceId())
                .then(attachmentAvailability.get(owner, attachmentId)))
            .map(result -> response(attachmentId, result.status()));
    }

    private MediaAvailabilityResponse response(UUID attachmentId, AttachmentAvailabilityStatus status) {
        return switch (status) {
            case READY -> new MediaAvailabilityResponse(attachmentId, MediaContractAvailability.READY, null, null, null, null);
            case PROCESSING, RESTORE_REQUIRED -> new MediaAvailabilityResponse(attachmentId, MediaContractAvailability.RESTORING, null, null, null, null);
            case MISSING -> new MediaAvailabilityResponse(attachmentId, MediaContractAvailability.MISSING, null, null, null, null);
            case CORRUPTED -> new MediaAvailabilityResponse(attachmentId, MediaContractAvailability.CORRUPTED, null, null, null, null);
        };
    }
}
