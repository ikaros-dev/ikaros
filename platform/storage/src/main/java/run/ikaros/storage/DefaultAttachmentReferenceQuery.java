package run.ikaros.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.ResourceOwnershipQuery;
import run.ikaros.storage.api.AttachmentReference;
import run.ikaros.storage.api.AttachmentReferenceQuery;

/** Storage-owned Attachment identity and ownership capability. */
@Service
final class DefaultAttachmentReferenceQuery implements AttachmentReferenceQuery {
    private final AttachmentRepository attachments;
    private final ResourceOwnershipQuery resources;

    DefaultAttachmentReferenceQuery(AttachmentRepository attachments, ResourceOwnershipQuery resources) {
        this.attachments = attachments;
        this.resources = resources;
    }

    @Override
    public Mono<AttachmentReference> requireReadable(UUID actorId, UUID attachmentId) {
        return attachments.findById(attachmentId)
            .filter(attachment -> attachment.archivedAt() == null && attachment.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或不可用")))
            .flatMap(attachment -> resources.requireOwned(actorId, attachment.resourceId())
                .thenReturn(new AttachmentReference(attachment.id(), attachment.resourceId())));
    }

    @Override
    public Mono<AttachmentReference> requireActiveForResource(UUID actorId, UUID resourceId, UUID attachmentId) {
        return resources.requireOwned(actorId, resourceId)
            .then(attachments.findByIdAndResourceIdAndArchivedAtIsNullAndDeletedAtIsNull(attachmentId, resourceId))
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或不属于指定 Resource")))
            .map(attachment -> new AttachmentReference(attachment.id(), attachment.resourceId()));
    }
}
