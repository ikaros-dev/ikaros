package run.ikaros.storage.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** Resolves current Attachment readability without exposing Blob or Provider internals. */
public interface AttachmentAvailabilityQuery {
    Mono<AttachmentAvailability> get(UUID actorId, UUID attachmentId);
}
