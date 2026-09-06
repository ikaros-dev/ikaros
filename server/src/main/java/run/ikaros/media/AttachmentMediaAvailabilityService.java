package run.ikaros.media;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.*;

@Service
public class AttachmentMediaAvailabilityService {
    private final AttachmentRepository attachments; private final MediaAvailabilityService availability;
    private final BlobRepository blobs; private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providerRegistry; private final StorageRestoreRequestRepository restoreRequests;
    private final StorageRestoreOperationRepository restoreOperations;
    public AttachmentMediaAvailabilityService(AttachmentRepository attachments, MediaAvailabilityService availability,
        BlobRepository blobs, BlobPlacementRepository placements, StorageProviderRegistry providerRegistry,
        StorageRestoreRequestRepository restoreRequests, StorageRestoreOperationRepository restoreOperations) { this.attachments=attachments; this.availability=availability;
        this.blobs=blobs; this.placements=placements; this.providerRegistry=providerRegistry; this.restoreRequests=restoreRequests; this.restoreOperations=restoreOperations; }
    public Mono<MediaAvailabilityResponse> get(UUID owner, UUID attachmentId) { return attachments.findById(attachmentId)
        .filter(a -> a.deletedAt()==null).switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
        .flatMap(a -> availability.get(owner,a.resourceId()).then(blobs.findById(a.blobId())
            .map(b -> new Ctx(attachmentId,b)).switchIfEmpty(Mono.error(new NotFoundException("附件引用的 Blob 不存在"))))
        ).flatMap(c -> response(owner,c.id(),c.blob())); }
    private Mono<MediaAvailabilityResponse> response(UUID owner, UUID id, BlobEntity blob) { return switch(blob.availability()) {
        case RESTORING, PROCESSING -> activeRestore(owner,id).map(r -> new MediaAvailabilityResponse(id,MediaContractAvailability.RESTORING,r.id(),null,null,null))
            .switchIfEmpty(Mono.just(new MediaAvailabilityResponse(id,MediaContractAvailability.RESTORING,null,null,null,null)));
        case MISSING -> Mono.just(new MediaAvailabilityResponse(id,MediaContractAvailability.MISSING,null,null,null,null));
        case CORRUPTED -> Mono.just(new MediaAvailabilityResponse(id,MediaContractAvailability.CORRUPTED,null,null,null,null));
        case AVAILABLE, REMOTE -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id()).collectList().flatMap(all -> Flux.fromIterable(all)
            .filterWhen(this::isReadablePlacement).concatMap(p -> providerRegistry.getByKey(p.provider())
                .map(v -> v.status()!=StorageProviderStatus.DISABLED && v.status()!=StorageProviderStatus.FAILED)).any(Boolean::booleanValue)
            .map(readable -> new MediaAvailabilityResponse(id,readable?MediaContractAvailability.READY:
                all.stream().anyMatch(p -> p.placementState()!=PlacementState.ACTIVE)?MediaContractAvailability.RESTORE_REQUIRED:MediaContractAvailability.MISSING,null,null,null,null))); }; }
    private Mono<Boolean> isReadablePlacement(BlobPlacementEntity placement) {
        if (placement.placementState() == PlacementState.ACTIVE) return Mono.just(true);
        if (placement.placementState() != PlacementState.READY_TEMPORARILY) return Mono.just(false);
        return restoreOperations.findFirstByPlacementIdOrderByRestoreGenerationDesc(placement.id())
            .map(o -> o.status() == StorageRestoreOperationStatus.READY_TEMPORARILY
                && o.restoreExpiresAt() != null && o.restoreExpiresAt().isAfter(java.time.Instant.now()))
            .defaultIfEmpty(false);
    }
    private Mono<StorageRestoreRequestEntity> activeRestore(UUID owner, UUID id) { return restoreRequests.findFirstByActorIdAndScopeAndScopeIdAndStatusInOrderByCreatedAtDesc(owner,
        StorageRestoreScope.ATTACHMENT,id,List.of(StorageRestoreRequestStatus.REQUESTED,StorageRestoreRequestStatus.IN_PROGRESS)); }
    private record Ctx(UUID id, BlobEntity blob) {}
}
