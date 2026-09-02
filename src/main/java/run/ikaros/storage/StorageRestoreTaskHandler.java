package run.ikaros.storage;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskDispatcher;

@Component
public class StorageRestoreTaskHandler {
    private final BackgroundTaskDispatcher dispatcher;
    private final StorageRestoreRequestRepository requests;
    private final AttachmentRepository attachments;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providers;
    private final StorageRestoreExecutor executor;

    public StorageRestoreTaskHandler(BackgroundTaskDispatcher dispatcher, StorageRestoreRequestRepository requests,
        AttachmentRepository attachments, BlobRepository blobs, BlobPlacementRepository placements,
        StorageProviderRegistry providers, StorageRestoreExecutor executor) {
        this.dispatcher = dispatcher; this.requests = requests; this.attachments = attachments;
        this.blobs = blobs; this.placements = placements; this.providers = providers; this.executor = executor;
    }

    @PostConstruct
    void register() { dispatcher.register("storage.restore", this::handle); }

    private Mono<Map<String, Object>> handle(BackgroundTask task) {
        UUID requestId = uuid(task.payload(), "restore_request_id");
        UUID attachmentId = uuid(task.payload(), "attachment_id");
        return requests.findById(requestId)
            .switchIfEmpty(Mono.error(new NotFoundException("Restore Request 不存在")))
            .flatMap(request -> updateStatus(request, StorageRestoreRequestStatus.IN_PROGRESS)
                .then(attachments.findById(attachmentId)
                    .switchIfEmpty(Mono.error(new NotFoundException("附件不存在"))))
                .flatMap(attachment -> restoreAttachment(request, attachment, requestId)));
    }

    private Mono<Map<String, Object>> restoreAttachment(StorageRestoreRequestEntity request,
        AttachmentEntity attachment, UUID requestId) {
        return blobs.findById(attachment.blobId())
            .switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .flatMap(blob -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                .filter(p -> p.placementState() != PlacementState.ACTIVE).next()
                .switchIfEmpty(Mono.error(new ConflictException("Blob 没有可恢复的 Placement")))
                .flatMap(placement -> providers.getByKey(placement.provider())
                    .flatMap(provider -> execute(request, requestId, blob, placement, provider))));
    }

    private Mono<Map<String, Object>> execute(StorageRestoreRequestEntity request, UUID requestId,
        BlobEntity blob, BlobPlacementEntity placement, StorageProvider provider) {
        if (!executor.supports(provider)) return Mono.error(new ConflictException("Provider 不支持恢复"));
        return executor.restore(provider, placement, blob).flatMap(result -> {
            if (!result.readable()) return Mono.error(new ConflictException("Provider 恢复后对象仍不可读"));
            Instant now = Instant.now();
            BlobPlacementEntity active = new BlobPlacementEntity(placement.id(), placement.blobId(), placement.provider(),
                placement.storageTier(), placement.objectKey(), PlacementState.ACTIVE, now, placement.createdAt(), placement.version());
            BlobEntity available = new BlobEntity(blob.id(), blob.hashAlgorithm(), blob.sha256(), blob.sizeBytes(),
                blob.mediaType(), BlobAvailability.AVAILABLE, blob.createdAt(), blob.version());
            return placements.save(active).then(blobs.save(available))
                .then(updateStatus(request, StorageRestoreRequestStatus.COMPLETED))
                .thenReturn(Map.<String, Object>of("restore_request_id", requestId.toString(), "completed_items", 1));
        });
    }

    private Mono<StorageRestoreRequestEntity> updateStatus(StorageRestoreRequestEntity request,
        StorageRestoreRequestStatus status) {
        return requests.save(new StorageRestoreRequestEntity(request.id(), request.actorId(), request.scope(), request.scopeId(),
            status, request.totalItems(), status == StorageRestoreRequestStatus.COMPLETED ? request.totalItems() : request.completedItems(),
            request.totalBytes(), request.errorSummary(), request.idempotencyKey(), request.backgroundTaskId(), request.createdAt(),
            Instant.now(), request.version()));
    }

    private UUID uuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) throw new IllegalArgumentException("Task Payload 缺少 " + key);
        return UUID.fromString(value.toString());
    }
}
