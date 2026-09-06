package run.ikaros.storage.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** Queries active Attachment identity and ownership with object-level authorization. */
public interface AttachmentReferenceQuery {
    Mono<AttachmentReference> requireReadable(UUID actorId, UUID attachmentId);

    Mono<AttachmentReference> requireActiveForResource(UUID actorId, UUID resourceId, UUID attachmentId);
}
