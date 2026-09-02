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
import run.ikaros.media.MediaEpisodeRepository;
import run.ikaros.event.DurableEventService;

@Component
public class StorageRestoreTaskHandler {
    private final BackgroundTaskDispatcher dispatcher;
    private final StorageRestoreRequestRepository requests;
    private final AttachmentRepository attachments;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providers;
    private final StorageRestoreExecutor executor;
    private final StorageRestoreOperationRepository operations;
    private final StorageRestoreRequestItemRepository items;
    private final MediaEpisodeRepository episodes;
    private final DurableEventService events;

    public StorageRestoreTaskHandler(BackgroundTaskDispatcher dispatcher, StorageRestoreRequestRepository requests,
        AttachmentRepository attachments, BlobRepository blobs, BlobPlacementRepository placements,
        StorageProviderRegistry providers, StorageRestoreExecutor executor,
        StorageRestoreOperationRepository operations, StorageRestoreRequestItemRepository items, MediaEpisodeRepository episodes,
        DurableEventService events) {
        this.dispatcher = dispatcher; this.requests = requests; this.attachments = attachments;
        this.blobs = blobs; this.placements = placements; this.providers = providers; this.executor = executor;
        this.operations = operations; this.items = items;
        this.episodes = episodes;
        this.events = events;
    }

    @PostConstruct
    void register() { dispatcher.register("storage.restore", this::handle); }

    private Mono<Map<String, Object>> handle(BackgroundTask task) {
        UUID requestId = uuid(task.payload(), "restore_request_id");
        String restoreClass = String.valueOf(task.payload().getOrDefault("provider_restore_class", "STANDARD"));
        boolean retryFailedOnly = Boolean.TRUE.equals(task.payload().get("retry_failed_only"));
        return requests.findById(requestId)
            .switchIfEmpty(Mono.error(new NotFoundException("Restore Request 不存在")))
            .flatMap(request -> updateStatus(request, StorageRestoreRequestStatus.IN_PROGRESS).then(
                task.payload().containsKey("season_id")
                    ? restoreSeason(request, requestId, task.id(), restoreClass, uuid(task.payload(), "season_id"), retryFailedOnly)
                    : attachments.findById(uuid(task.payload(), "attachment_id"))
                        .filter(attachment -> attachment.deletedAt() == null)
                        .switchIfEmpty(Mono.error(new NotFoundException("附件不存在")))
                        .flatMap(attachment -> restoreAttachment(request, attachment, requestId, task.id(), restoreClass, true,
                            retryFailedOnly))));
    }

    private Mono<Map<String, Object>> restoreSeason(StorageRestoreRequestEntity request, UUID requestId, UUID taskId,
        String restoreClass, UUID seasonId, boolean retryFailedOnly) {
        return episodes.findAllByOwnerIdAndSeasonIdOrderByEpisodeNumberAsc(request.actorId(), seasonId)
            .flatMap(episode -> attachments.findAllByResourceIdAndDeletedAtIsNullOrderByCreatedAtAsc(episode.resourceId()))
            .concatMap(attachment -> restoreAttachment(request, attachment, requestId, taskId, restoreClass, false, retryFailedOnly))
            .collectList()
            .then(updateStatus(request, StorageRestoreRequestStatus.COMPLETED))
            .thenReturn(Map.of("restore_request_id", requestId.toString(), "completed_items", request.totalItems()));
    }

    private Mono<Map<String, Object>> restoreAttachment(StorageRestoreRequestEntity request,
        AttachmentEntity attachment, UUID requestId, UUID taskId, String restoreClass) {
        return restoreAttachment(request, attachment, requestId, taskId, restoreClass, true, false);
    }

    private Mono<Map<String, Object>> restoreAttachment(StorageRestoreRequestEntity request,
        AttachmentEntity attachment, UUID requestId, UUID taskId, String restoreClass, boolean completeRequest) {
        return restoreAttachment(request, attachment, requestId, taskId, restoreClass, completeRequest, false);
    }

    private Mono<Map<String, Object>> restoreAttachment(StorageRestoreRequestEntity request,
        AttachmentEntity attachment, UUID requestId, UUID taskId, String restoreClass, boolean completeRequest,
        boolean retryFailedOnly) {
        return blobs.findById(attachment.blobId())
            .switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .flatMap(blob -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                .filter(p -> p.placementState() != PlacementState.ACTIVE).next()
                .switchIfEmpty(Mono.error(new ConflictException("Blob 没有可恢复的 Placement")))
                .flatMap(placement -> operation(request, placement, taskId, restoreClass)
                    .flatMap(operation -> items.findByRequestIdAndPlacementId(request.id(), placement.id())
                        .map(existing -> !retryFailedOnly || existing.status() == StorageRestoreRequestItemStatus.FAILED)
                        .defaultIfEmpty(!retryFailedOnly)
                        .flatMap(allowed -> allowed
                            ? item(request, placement, operation)
                                .then(execute(request, requestId, blob, placement, operation, restoreClass, completeRequest))
                            : Mono.empty()))));
    }

    private Mono<Map<String, Object>> execute(StorageRestoreRequestEntity request, UUID requestId,
        BlobEntity blob, BlobPlacementEntity placement, StorageRestoreOperationEntity operation, String restoreClass,
        boolean completeRequest) {
        if (operation.status() == StorageRestoreOperationStatus.SUCCEEDED) {
            Mono<Void> status = completeRequest ? updateStatus(request, StorageRestoreRequestStatus.COMPLETED).then() : Mono.empty();
            return updateItem(request.id(), placement.id(), StorageRestoreRequestItemStatus.READY, null)
                .then(status)
                .thenReturn(Map.<String, Object>of("restore_request_id", requestId.toString(), "completed_items", 1));
        }
        return updateOperation(operation, StorageRestoreOperationStatus.IN_PROGRESS, null, null)
            .flatMap(started -> events.append("storage.restore-operation.started", 1, "restore_operation", started.id(),
                "{\"operation_id\":\"" + started.id() + "\",\"placement_id\":\"" + started.placementId()
                    + "\",\"restore_class\":\"" + started.providerRestoreClass() + "\",\"size_bytes\":" + blob.sizeBytes() + "}").then())
            .then(providers.getByKey(placement.provider()))
            .flatMap(provider -> {
                if (!executor.supports(provider)) return Mono.error(new ConflictException("Provider 不支持恢复"));
                return executor.restore(provider, placement, blob);
            }).flatMap(result -> {
            if (!result.readable()) return Mono.error(new ConflictException("Provider 恢复后对象仍不可读"));
            Instant now = Instant.now();
            BlobPlacementEntity active = new BlobPlacementEntity(placement.id(), placement.blobId(), placement.provider(),
                placement.storageTier(), placement.objectKey(), PlacementState.ACTIVE, now, placement.createdAt(), placement.version());
            BlobEntity available = new BlobEntity(blob.id(), blob.hashAlgorithm(), blob.sha256(), blob.sizeBytes(),
                blob.mediaType(), BlobAvailability.AVAILABLE, blob.createdAt(), blob.version());
            Mono<Void> status = completeRequest ? updateStatus(request, StorageRestoreRequestStatus.COMPLETED).then() : Mono.empty();
            return placements.save(active).then(blobs.save(available))
                .then(updateOperation(operation, StorageRestoreOperationStatus.SUCCEEDED, result.providerOperationId(), result.expiresAt()))
                .flatMap(ready -> events.append("storage.restore-operation.ready", 1, "restore_operation", ready.id(),
                    "{\"operation_id\":\"" + ready.id() + "\",\"placement_id\":\"" + ready.placementId()
                        + "\",\"restore_expires_at\":" + (ready.restoreExpiresAt() == null ? "null" : "\"" + ready.restoreExpiresAt() + "\"") + "}").then())
                .then(updateItem(request.id(), placement.id(), StorageRestoreRequestItemStatus.READY, null))
                .then(status)
                .thenReturn(Map.<String, Object>of("restore_request_id", requestId.toString(), "completed_items", 1));
        }).onErrorResume(error -> updateOperation(operation, StorageRestoreOperationStatus.FAILED, null, null)
            .flatMap(failed -> events.append("storage.restore-operation.failed", 1, "restore_operation", failed.id(),
                "{\"operation_id\":\"" + failed.id() + "\",\"placement_id\":\"" + failed.placementId()
                    + "\",\"error_code\":\"restore-failed\",\"retryable\":false}").then())
            .then(updateItem(request.id(), placement.id(), StorageRestoreRequestItemStatus.FAILED, error.getMessage()))
            .then(updateStatus(request, StorageRestoreRequestStatus.FAILED))
            .then(Mono.error(error)));
    }

    private Mono<StorageRestoreOperationEntity> operation(StorageRestoreRequestEntity request,
        BlobPlacementEntity placement, UUID taskId, String restoreClass) {
        return operations.findByPlacementIdAndProviderRestoreClassAndRestoreGeneration(placement.id(), restoreClass, 0)
            .switchIfEmpty(Mono.defer(() -> {
                Instant now = Instant.now();
                return operations.save(new StorageRestoreOperationEntity(null, placement.id(), restoreClass, 0,
                    StorageRestoreOperationStatus.REQUESTED, taskId, null, null, null, now, now, null));
            }));
    }

    private Mono<Void> item(StorageRestoreRequestEntity request, BlobPlacementEntity placement,
        StorageRestoreOperationEntity operation) {
        return items.findByRequestIdAndPlacementId(request.id(), placement.id())
            .switchIfEmpty(Mono.defer(() -> {
                Instant now = Instant.now();
                return items.save(new StorageRestoreRequestItemEntity(null, request.id(), placement.id(), operation.id(),
                    StorageRestoreRequestItemStatus.WAITING, null, now, now, null));
            })).then();
    }

    private Mono<Void> updateItem(UUID requestId, UUID placementId, StorageRestoreRequestItemStatus status, String error) {
        return items.findByRequestIdAndPlacementId(requestId, placementId)
            .flatMap(old -> items.save(new StorageRestoreRequestItemEntity(old.id(), old.requestId(), old.placementId(),
                old.operationId(), status, error, old.createdAt(), Instant.now(), old.version()))).then();
    }

    private Mono<StorageRestoreOperationEntity> updateOperation(StorageRestoreOperationEntity old,
        StorageRestoreOperationStatus status, String providerOperationId, Instant expiresAt) {
        return operations.save(new StorageRestoreOperationEntity(old.id(), old.placementId(), old.providerRestoreClass(),
            old.restoreGeneration(), status, old.backgroundTaskId(), providerOperationId == null ? old.providerOperationId() : providerOperationId,
            expiresAt == null ? old.restoreExpiresAt() : expiresAt, old.errorSummary(), old.createdAt(), Instant.now(), old.version()));
    }

    private Mono<StorageRestoreRequestEntity> updateStatus(StorageRestoreRequestEntity request,
        StorageRestoreRequestStatus status) {
        return requests.save(new StorageRestoreRequestEntity(request.id(), request.actorId(), request.scope(), request.scopeId(),
            status, request.totalItems(), status == StorageRestoreRequestStatus.COMPLETED ? request.totalItems() : request.completedItems(),
            request.totalBytes(), request.errorSummary(), request.idempotencyKey(), request.backgroundTaskId(), request.createdAt(),
            Instant.now(), request.version()))
            .flatMap(saved -> status == StorageRestoreRequestStatus.COMPLETED || status == StorageRestoreRequestStatus.PARTIAL_FAILURE
                ? events.append("storage.restore-request.completed", 1, "restore_request", saved.id(),
                    "{\"request_id\":\"" + saved.id() + "\",\"status\":\"" + saved.status()
                        + "\",\"ready_items\":" + saved.completedItems() + ",\"failed_items\":"
                        + Math.max(0, saved.totalItems() - saved.completedItems()) + "}").thenReturn(saved)
                : Mono.just(saved));
    }

    private UUID uuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) throw new IllegalArgumentException("Task Payload 缺少 " + key);
        return UUID.fromString(value.toString());
    }
}
