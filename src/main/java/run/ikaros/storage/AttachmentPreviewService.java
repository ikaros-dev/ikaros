package run.ikaros.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.StorageUnavailableException;
import run.ikaros.resource.ResourceRepository;

@Service
public class AttachmentPreviewService {
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providers;
    private final StorageObjectProviderRegistry objects;
    private final DeliveryGrantService deliveryGrants;
    private final DeliveryLeaseService deliveryLeases;
    private final DeliveryGrantContractService deliveryContracts;

    public AttachmentPreviewService(AttachmentRepository attachments, ResourceRepository resources, BlobRepository blobs,
                                     BlobPlacementRepository placements, StorageProviderRegistry providers,
                                     StorageObjectProviderRegistry objects, DeliveryGrantService deliveryGrants,
                                     DeliveryLeaseService deliveryLeases, DeliveryGrantContractService deliveryContracts) {
        this.attachments = attachments; this.resources = resources; this.blobs = blobs; this.placements = placements;
        this.providers = providers; this.objects = objects; this.deliveryGrants = deliveryGrants;
        this.deliveryLeases = deliveryLeases; this.deliveryContracts = deliveryContracts;
    }

    public Mono<AttachmentPreviewUrlView> issue(UUID actorId, UUID attachmentId) {
        return attachments.findById(attachmentId).filter(a -> a.deletedAt() == null)
            .flatMap(attachment -> resources.findByIdAndOwnerId(attachment.resourceId(), actorId).thenReturn(attachment))
            .switchIfEmpty(Mono.error(new NotFoundException("Attachment 不存在或无权访问")))
            .flatMap(attachment -> blobs.findById(attachment.blobId())
                .switchIfEmpty(Mono.error(new NotFoundException("Attachment 对应的 Blob 不存在")))
                .flatMap(blob -> preferDeliveryBinding(actorId, attachmentId, blob)));
    }

    private Mono<AttachmentPreviewUrlView> preferDeliveryBinding(UUID actorId, UUID attachmentId, BlobEntity blob) {
        DeliveryGrantRequest request = new DeliveryGrantRequest(900, null, null, DeliveryIntent.PLAYBACK, null, true);
        return deliveryGrants.issue(actorId, attachmentId, request)
            .flatMap(grant -> deliveryLeases.create(actorId, attachmentId,
                    new DeliveryLeaseRequest(grant.token(), request.ttlSeconds()))
                .flatMap(lease -> deliveryContracts.contract(attachmentId, grant, lease)
                    .switchIfEmpty(Mono.error(new StorageUnavailableException("附件没有可用 Delivery Binding"))))
                .map(contract -> new AttachmentPreviewUrlView(contract.method(), contract.url(), contract.expiresAt(),
                    contract.rangeSupported(), contract.contentType()))
                .onErrorResume(error -> deliveryGrants.revoke(actorId, grant.id())
                    .onErrorResume(revokeError -> Mono.empty())
                    .then(Mono.error(error))))
            .onErrorResume(error -> isMissingDeliveryBinding(error)
                ? fallbackToStorage(blob)
                : Mono.error(error));
    }

    private Mono<AttachmentPreviewUrlView> fallbackToStorage(BlobEntity blob) {
        return placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                    .filter(placement -> placement.placementState() == PlacementState.ACTIVE)
                    .concatMap(placement -> providers.getByKey(placement.provider())
                        .flatMap(provider -> objects.createReadIntent(provider, placement.objectKey())
                            .map(intent -> new AttachmentPreviewUrlView(intent.method(), intent.url(), intent.expiresAt(), true,
                                blob.mediaType()))))
                    .next()
                    .switchIfEmpty(Mono.error(new StorageUnavailableException("附件没有可用存储 Placement")));
    }

    private boolean isMissingDeliveryBinding(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof StorageUnavailableException
                && "附件没有可用 Delivery Binding".equals(current.getMessage())) return true;
            current = current.getCause();
        }
        return false;
    }
}
