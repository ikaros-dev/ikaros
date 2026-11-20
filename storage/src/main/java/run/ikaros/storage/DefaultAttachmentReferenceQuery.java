package run.ikaros.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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
        return attachments.findReadableReferenceById(attachmentId)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或不可用")))
            .flatMap(attachment -> attachment.resourceId() == null
                ? Mono.just(new AttachmentReference(attachment.id(), null))
                : resources.requireReadable(actorId, attachment.resourceId())
                    .thenReturn(new AttachmentReference(attachment.id(), attachment.resourceId())));
    }

    @Override
    public Mono<AttachmentReference> requireActiveForResource(UUID actorId, UUID resourceId, UUID attachmentId) {
        return resources.requireReadable(actorId, resourceId)
            .then(attachments.findActiveReferenceByIdAndResourceId(attachmentId, resourceId))
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或不属于指定 Resource")))
            .map(attachment -> new AttachmentReference(attachment.id(), attachment.resourceId()));
    }

    @Override
    public Flux<AttachmentReference> listActiveForResource(UUID actorId, UUID resourceId) {
        return resources.requireReadable(actorId, resourceId)
            .thenMany(attachments.findActiveReferencesByResourceId(resourceId)
                .map(attachment -> new AttachmentReference(attachment.id(), attachment.resourceId())));
    }
}
