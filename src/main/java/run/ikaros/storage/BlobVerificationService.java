package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;
import run.ikaros.resource.ResourceRepository;

@Service
public class BlobVerificationService {
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final StorageProviderRegistry providers;
    private final List<StorageContentReader> readers;
    private final BlobIntegrityService integrity;
    private final DurableEventService events;
    private final TransactionalOperator transaction;

    public BlobVerificationService(BlobRepository blobs, BlobPlacementRepository placements, AttachmentRepository attachments,
        ResourceRepository resources, StorageProviderRegistry providers, List<StorageContentReader> readers,
        BlobIntegrityService integrity, DurableEventService events, TransactionalOperator transaction) {
        this.blobs = blobs; this.placements = placements; this.attachments = attachments; this.resources = resources;
        this.providers = providers; this.readers = List.copyOf(readers); this.integrity = integrity;
        this.events = events; this.transaction = transaction;
    }

    public Mono<BlobVerificationView> verify(UUID actorId, UUID blobId) {
        return ownedBlob(actorId, blobId)
            .flatMap(blob -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                .filter(p -> p.placementState() == PlacementState.ACTIVE).next()
                .switchIfEmpty(Mono.error(new NotFoundException("Blob 当前没有可校验的可读副本")))
                .flatMap(placement -> providers.getByKey(placement.provider())
                    .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
                    .flatMap(provider -> readers.stream().filter(reader -> reader.supports(provider)).findFirst()
                        .map(reader -> reader.read(provider, placement, blob, null)).orElse(Mono.error(new NotFoundException("没有匹配的 Storage Content Reader")))
                        .flatMap(content -> integrity.verify(blob.id(), blob.sha256(), blob.sizeBytes(), content.body())
                            .flatMap(result -> persist(blob, placement, result))))));
    }

    private Mono<BlobVerificationView> persist(BlobEntity blob, BlobPlacementEntity placement, BlobIntegrityResult result) {
        Instant now = Instant.now();
        BlobAvailability availability = result.status() == BlobIntegrityStatus.VERIFIED
            ? BlobAvailability.AVAILABLE : BlobAvailability.CORRUPTED;
        BlobPlacementEntity updatedPlacement = new BlobPlacementEntity(placement.id(), placement.blobId(), placement.provider(),
            placement.storageTier(), placement.objectKey(), placement.placementState(),
            result.status() == BlobIntegrityStatus.VERIFIED ? now : placement.verifiedAt(), placement.createdAt(), placement.version());
        BlobEntity updatedBlob = new BlobEntity(blob.id(), blob.hashAlgorithm(), blob.sha256(), blob.sizeBytes(), blob.mediaType(),
            availability, blob.createdAt(), blob.version());
        String eventType = result.status() == BlobIntegrityStatus.VERIFIED
            ? "storage.blob.verified" : "storage.blob.integrity-failed";
        String payload = "{\"blob_id\":\"" + blob.id() + "\",\"placement_id\":\"" + placement.id()
            + "\",\"integrity_status\":\"" + result.status() + "\",\"verified_at\":\"" + now
            + "\",\"actual_sha256\":\"" + result.actualSha256() + "\",\"actual_size\":" + result.actualSize() + "}";
        return transaction.transactional(placements.save(updatedPlacement).then(blobs.save(updatedBlob))
            .then(events.append(eventType, 1, "blob", blob.id(), payload)).thenReturn(
                new BlobVerificationView(blob.id(), placement.id(), result.status(), result.actualSha256(), result.actualSize(), now)));
    }

    private Mono<BlobEntity> ownedBlob(UUID actorId, UUID blobId) {
        return blobs.findById(blobId).switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .flatMap(blob -> attachments.findFirstByBlobIdAndArchivedAtIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(blob.id())
                .flatMap(attachment -> resources.findByIdAndOwnerId(attachment.resourceId(), actorId).thenReturn(blob)))
            .switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在或无权访问")));
    }
}
