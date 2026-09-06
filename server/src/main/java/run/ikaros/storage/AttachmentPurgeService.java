package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;
import run.ikaros.resource.ResourceRepository;

@Service
public class AttachmentPurgeService {
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final AuditService audit;
    private final DurableEventService events;
    private final TransactionalOperator transaction;

    public AttachmentPurgeService(AttachmentRepository attachments, ResourceRepository resources, AuditService audit,
                                  DurableEventService events, TransactionalOperator transaction) {
        this.attachments = attachments; this.resources = resources; this.audit = audit;
        this.events = events; this.transaction = transaction;
    }

    public Mono<Void> purge(UUID actorId, UUID resourceId, UUID attachmentId) {
        return resources.findByIdAndOwnerId(resourceId, actorId)
            .switchIfEmpty(Mono.error(new NotFoundException("资源不存在或无权访问")))
            .then(attachments.findById(attachmentId)
                .filter(a -> a.resourceId().equals(resourceId))
                .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或无权访问"))))
            .flatMap(attachment -> {
                if (attachment.deletedAt() == null)
                    return Mono.error(new ConflictException("Attachment 必须先软删除后才能 Purge"));
                String payload = "{\"attachment_id\":\"" + attachment.id() + "\",\"resource_id\":\""
                    + resourceId + "\",\"blob_id\":\"" + attachment.blobId() + "\",\"purged_at\":\""
                    + Instant.now() + "\"}";
                return transaction.transactional(attachments.delete(attachment)
                    .then(audit.record(actorId, "attachment.purge", "ATTACHMENT", attachment.id(), "{}"))
                    .then(events.append("attachment.purged", 1, "attachment", attachment.id(), payload))).then();
            });
    }
}
