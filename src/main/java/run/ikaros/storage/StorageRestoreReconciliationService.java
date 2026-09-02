package run.ikaros.storage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.event.DurableEventService;

@Service
public class StorageRestoreReconciliationService {
    private final StorageRestoreOperationRepository operations;
    private final BlobPlacementRepository placements;
    private final BlobRepository blobs;
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final StorageProviderRegistry providers;
    private final List<StorageRestoreStatusQuery> queries;
    private final DurableEventService events;

    public StorageRestoreReconciliationService(StorageRestoreOperationRepository operations, BlobPlacementRepository placements,
        BlobRepository blobs, AttachmentRepository attachments, ResourceRepository resources, StorageProviderRegistry providers,
        List<StorageRestoreStatusQuery> queries, DurableEventService events) {
        this.operations = operations; this.placements = placements; this.blobs = blobs; this.attachments = attachments;
        this.resources = resources; this.providers = providers; this.queries = queries; this.events = events;
    }

    public Mono<StorageRestoreOperationView> reconcile(UUID actorId, UUID operationId) {
        return operations.findById(operationId).switchIfEmpty(Mono.error(new NotFoundException("Restore Operation 不存在")))
            .flatMap(operation -> placements.findById(operation.placementId())
                .switchIfEmpty(Mono.error(new NotFoundException("Restore Placement 不存在")))
                .flatMap(placement -> blobs.findById(placement.blobId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Restore Blob 不存在")))
                    .flatMap(blob -> ownerCanAccess(actorId, blob.id()).thenReturn(new Context(operation, placement, blob)))))
            .flatMap(context -> events.append("storage.restore.reconcile-requested", 1,
                    "STORAGE_RESTORE_OPERATION", context.operation.id(),
                    "{\"operation_id\":\"" + context.operation.id() + "\"}")
                .then(queryAndApply(context)))
            .flatMap(context -> events.append("storage.restore.reconciled", 1,
                    "STORAGE_RESTORE_OPERATION", context.operation.id(),
                    "{\"operation_id\":\"" + context.operation.id() + "\",\"status\":\""
                        + context.operation.status() + "\"}").thenReturn(context))
            .map(this::view);
    }

    private Mono<Context> queryAndApply(Context context) {
        return providers.getByKey(context.placement.provider()).switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .flatMap(provider -> queries.stream().filter(query -> query.supports(provider)).findFirst()
                .map(query -> query.query(provider, context.placement, context.blob))
                .orElseGet(() -> Mono.just(StorageRestoreProviderStatus.UNKNOWN)))
            .flatMap(status -> switch (status) {
                case READABLE -> activate(context);
                case NOT_READABLE -> fail(context, "Provider 对账确认对象不可读");
                case RESTORING, UNKNOWN -> Mono.just(context);
            });
    }

    private Mono<Context> activate(Context c) {
        Instant now = Instant.now();
        BlobPlacementEntity active = new BlobPlacementEntity(c.placement.id(), c.placement.blobId(), c.placement.provider(),
            c.placement.storageTier(), c.placement.objectKey(), PlacementState.ACTIVE, now, c.placement.createdAt(), c.placement.version());
        BlobEntity blob = new BlobEntity(c.blob.id(), c.blob.hashAlgorithm(), c.blob.sha256(), c.blob.sizeBytes(),
            c.blob.mediaType(), BlobAvailability.AVAILABLE, c.blob.createdAt(), c.blob.version());
        StorageRestoreOperationEntity operation = updated(c.operation, StorageRestoreOperationStatus.SUCCEEDED, null);
        return placements.save(active).then(blobs.save(blob)).then(operations.save(operation)).thenReturn(new Context(operation, active, blob));
    }

    private Mono<Context> fail(Context c, String reason) {
        StorageRestoreOperationEntity operation = updated(c.operation, StorageRestoreOperationStatus.FAILED, reason);
        return operations.save(operation).thenReturn(new Context(operation, c.placement, c.blob));
    }

    private StorageRestoreOperationEntity updated(StorageRestoreOperationEntity old, StorageRestoreOperationStatus status, String error) {
        return new StorageRestoreOperationEntity(old.id(), old.placementId(), old.providerRestoreClass(), old.restoreGeneration(),
            status, old.backgroundTaskId(), old.providerOperationId(), old.restoreExpiresAt(), error, old.createdAt(), Instant.now(), old.version());
    }

    private Mono<Void> ownerCanAccess(UUID actorId, UUID blobId) {
        return attachments.findAllByBlobIdAndDeletedAtIsNull(blobId)
            .flatMap(a -> resources.findByIdAndOwnerId(a.resourceId(), actorId))
            .next()
            .switchIfEmpty(Mono.error(new ConflictException("Restore Operation 无权访问"))).then();
    }

    private StorageRestoreOperationView view(Context c) {
        var o = c.operation;
        return new StorageRestoreOperationView(o.id(), o.placementId(), o.providerRestoreClass(), o.restoreGeneration(), o.status(),
            o.providerOperationId(), o.restoreExpiresAt(), o.errorSummary(), o.createdAt(), o.updatedAt(), o.version());
    }

    private record Context(StorageRestoreOperationEntity operation, BlobPlacementEntity placement, BlobEntity blob) {}
}
