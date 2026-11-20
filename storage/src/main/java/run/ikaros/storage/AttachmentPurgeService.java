package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.operations.api.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.resource.api.ResourceOwnershipQuery;

@Service
public class AttachmentPurgeService {
    private final AttachmentRepository attachments;
    private final ResourceAttachmentRepository resourceAttachments;
    private final ResourceOwnershipQuery resources;
    private final AuditService audit;
    private final DurableEventPublisher events;
    private final TransactionalOperator transaction;

    public AttachmentPurgeService(AttachmentRepository attachments, ResourceAttachmentRepository resourceAttachments,
                                  ResourceOwnershipQuery resources, AuditService audit,
                                  DurableEventPublisher events, TransactionalOperator transaction) {
        this.attachments = attachments; this.resourceAttachments = resourceAttachments; this.resources = resources; this.audit = audit;
        this.events = events; this.transaction = transaction;
    }

    public Mono<Void> purge(UUID actorId, UUID resourceId, UUID attachmentId) {
        return resources.requireOwned(actorId, resourceId)
            .then(attachments.findReferenceByIdAndResourceId(attachmentId, resourceId)
                .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或无权访问"))))
            .flatMap(attachment -> {
                if (attachment.status() != 2)
                    return Mono.error(new ConflictException("Attachment 必须先软删除后才能 Purge"));
                String payload = "{\"attachment_id\":\"" + attachment.id() + "\",\"resource_id\":\""
                    + resourceId + "\",\"blob_id\":\"" + attachment.blobId() + "\",\"purged_at\":\""
                    + Instant.now() + "\"}";
                return attachments.findById(attachment.id()).flatMap(current -> transaction.transactional(
                    attachments.save(new AttachmentEntity(current.id(), current.name(), current.attachmentKind(), 0,
                        current.createdBy(), current.idempotencyKey(), current.requestFingerprint(), current.createdAt(),
                        Instant.now(), current.version()))
                    .then(resourceAttachments.deleteByResourceIdAndAttachmentId(resourceId, attachment.id()))
                    .then(audit.record(actorId, "attachment.purge", "ATTACHMENT", attachment.id(), "{}"))
                    .then(events.append(new EventAppendRequest("attachment.purged", 1, "storage", "attachment", attachment.id(), payload))))).then();
            });
    }
}
