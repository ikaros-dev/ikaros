package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.storage.api.AttachmentAvailability;
import run.ikaros.storage.api.AttachmentAvailabilityQuery;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentReferenceQuery;
import run.ikaros.storage.api.BlobAvailability;
import run.ikaros.storage.api.PlacementState;

/** Storage-owned capability for resolving the current attachment availability. */
@Service
final class DefaultAttachmentAvailabilityQuery implements AttachmentAvailabilityQuery {
    private final AttachmentReferenceQuery references;
    private final AttachmentRepository attachments;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providers;
    private final StorageRestoreOperationRepository restoreOperations;

    DefaultAttachmentAvailabilityQuery(AttachmentReferenceQuery references, AttachmentRepository attachments,
        BlobRepository blobs, BlobPlacementRepository placements, StorageProviderRegistry providers,
        StorageRestoreOperationRepository restoreOperations) {
        this.references = references;
        this.attachments = attachments;
        this.blobs = blobs;
        this.placements = placements;
        this.providers = providers;
        this.restoreOperations = restoreOperations;
    }

    @Override
    public Mono<AttachmentAvailability> get(UUID actorId, UUID attachmentId) {
        return references.requireReadable(actorId, attachmentId)
            .then(attachments.findById(attachmentId))
            .flatMap(attachment -> blobs.findById(attachment.blobId()))
            .flatMap(blob -> switch (blob.availability()) {
                case PROCESSING -> Mono.just(result(attachmentId, AttachmentAvailabilityStatus.PROCESSING));
                case RESTORING -> Mono.just(result(attachmentId, AttachmentAvailabilityStatus.RESTORE_REQUIRED));
                case MISSING -> Mono.just(result(attachmentId, AttachmentAvailabilityStatus.MISSING));
                case CORRUPTED -> Mono.just(result(attachmentId, AttachmentAvailabilityStatus.CORRUPTED));
                case AVAILABLE, REMOTE -> placementStatus(attachmentId, blob.id());
            })
            .defaultIfEmpty(result(attachmentId, AttachmentAvailabilityStatus.MISSING));
    }

    private Mono<AttachmentAvailability> placementStatus(UUID attachmentId, UUID blobId) {
        return placements.findAllByBlobIdOrderByCreatedAtAsc(blobId)
            .collectList()
            .flatMap(all -> placementsStatus(attachmentId, all));
    }

    private Mono<AttachmentAvailability> placementsStatus(UUID attachmentId, java.util.List<BlobPlacementEntity> all) {
        return reactor.core.publisher.Flux.fromIterable(all)
            .filter(placement -> placement.placementState() == PlacementState.ACTIVE
                || placement.placementState() == PlacementState.READY_TEMPORARILY)
            .filterWhen(this::hasReadableProvider)
            .hasElements()
            .map(readable -> result(attachmentId, readable
                ? AttachmentAvailabilityStatus.READY
                : all.stream().anyMatch(p -> p.placementState() != PlacementState.ACTIVE)
                    ? AttachmentAvailabilityStatus.RESTORE_REQUIRED
                    : AttachmentAvailabilityStatus.MISSING));
    }

    private Mono<Boolean> hasReadableProvider(BlobPlacementEntity placement) {
        Mono<Boolean> providerReadable = providers.getByKey(placement.provider())
            .map(provider -> provider.status() != StorageProviderStatus.DISABLED
                && provider.status() != StorageProviderStatus.FAILED)
            .defaultIfEmpty(false);
        if (placement.placementState() == PlacementState.ACTIVE) {
            return providerReadable;
        }
        return restoreOperations.findFirstByPlacementIdOrderByRestoreGenerationDesc(placement.id())
            .filter(operation -> operation.status() == StorageRestoreOperationStatus.READY_TEMPORARILY
                && operation.restoreExpiresAt() != null && operation.restoreExpiresAt().isAfter(Instant.now()))
            .flatMap(operation -> providerReadable)
            .defaultIfEmpty(false);
    }

    private AttachmentAvailability result(UUID attachmentId, AttachmentAvailabilityStatus status) {
        return new AttachmentAvailability(attachmentId, status);
    }
}
