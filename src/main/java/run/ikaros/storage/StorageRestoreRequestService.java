package run.ikaros.storage;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.task.BackgroundTaskService;

@Service
public class StorageRestoreRequestService {
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageRestoreRequestRepository requests;
    private final BackgroundTaskService tasks;

    public StorageRestoreRequestService(AttachmentRepository attachments, ResourceRepository resources,
        BlobRepository blobs, BlobPlacementRepository placements, StorageRestoreRequestRepository requests,
        BackgroundTaskService tasks) {
        this.attachments = attachments; this.resources = resources; this.blobs = blobs;
        this.placements = placements; this.requests = requests; this.tasks = tasks;
    }

    public Mono<StorageRestoreRequestView> requestAttachment(UUID actorId, RequestAttachmentRestore request,
        String idempotencyKey) {
        if (idempotencyKey != null && idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("Idempotency-Key 不能为空字符串"));
        }
        Mono<StorageRestoreRequestEntity> existing = idempotencyKey == null ? Mono.empty()
            : requests.findByActorIdAndScopeAndScopeIdAndIdempotencyKey(actorId, StorageRestoreScope.ATTACHMENT,
                request.attachmentId(), idempotencyKey);
        return existing.switchIfEmpty(Mono.defer(() -> authorizedAttachment(actorId, request.attachmentId())
            .flatMap(attachment -> blobs.findById(attachment.blobId())
                .switchIfEmpty(Mono.error(new ConflictException("附件引用了不存在的 Blob")))
                .flatMap(blob -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                    .filter(p -> p.placementState() == PlacementState.ACTIVE).hasElements()
                    .flatMap(readable -> {
                        if (readable) return Mono.error(new ConflictException("附件已经存在可读副本"));
                        Instant now = Instant.now();
                        return requests.save(new StorageRestoreRequestEntity(null, actorId, StorageRestoreScope.ATTACHMENT,
                            request.attachmentId(), StorageRestoreRequestStatus.REQUESTED, 1, 0, blob.sizeBytes(), null,
                            idempotencyKey, null, now, now, null));
                    }))
                .flatMap(saved -> tasks.submit("storage.restore", Map.of("restore_request_id", saved.id().toString(),
                    "attachment_id", request.attachmentId().toString(), "provider_restore_class",
                    request.providerRestoreClass() == null ? "STANDARD" : request.providerRestoreClass()),
                    "storage.restore:" + saved.id()).flatMap(task -> requests.save(new StorageRestoreRequestEntity(
                        saved.id(), saved.actorId(), saved.scope(), saved.scopeId(), saved.status(), saved.totalItems(),
                        saved.completedItems(), saved.totalBytes(), saved.errorSummary(), saved.idempotencyKey(), task.id(),
                        saved.createdAt(), Instant.now(), saved.version())))))))
            .map(this::view);
    }

    public Mono<StorageRestoreRequestView> get(UUID actorId, UUID id) {
        return requests.findById(id).filter(r -> r.actorId().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Restore Request 不存在或无权访问"))).map(this::view);
    }

    public Flux<StorageRestoreRequestView> list(UUID actorId) {
        return requests.findAllByActorIdOrderByCreatedAtDesc(actorId).map(this::view);
    }

    private Mono<AttachmentEntity> authorizedAttachment(UUID actorId, UUID id) {
        return attachments.findById(id).filter(a -> a.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
            .flatMap(a -> resources.findByIdAndOwnerId(a.resourceId(), actorId)
                .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或无权访问"))).thenReturn(a));
    }

    private StorageRestoreRequestView view(StorageRestoreRequestEntity r) {
        return new StorageRestoreRequestView(r.id(), r.actorId(), r.scope(), r.scopeId(), r.status(), r.totalItems(),
            r.completedItems(), r.totalBytes(), r.errorSummary(), r.backgroundTaskId(), r.createdAt(), r.updatedAt());
    }
}
