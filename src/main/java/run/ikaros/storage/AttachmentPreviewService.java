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
    private final MediaDeliveryBindingRepository bindings;
    private final DeliveryProviderRepository deliveryProviders;
    private final DeliveryGrantService deliveryGrants;
    private final DeliveryLeaseService deliveryLeases;
    private final DeliveryGrantContractService deliveryContracts;

    public AttachmentPreviewService(AttachmentRepository attachments, ResourceRepository resources, BlobRepository blobs,
                                     BlobPlacementRepository placements, StorageProviderRegistry providers,
                                     StorageObjectProviderRegistry objects, MediaDeliveryBindingRepository bindings,
                                     DeliveryProviderRepository deliveryProviders, DeliveryGrantService deliveryGrants,
                                     DeliveryLeaseService deliveryLeases, DeliveryGrantContractService deliveryContracts) {
        this.attachments = attachments; this.resources = resources; this.blobs = blobs; this.placements = placements;
        this.providers = providers; this.objects = objects; this.bindings = bindings; this.deliveryProviders = deliveryProviders;
        this.deliveryGrants = deliveryGrants;
        this.deliveryLeases = deliveryLeases; this.deliveryContracts = deliveryContracts;
    }

    public Mono<AttachmentPreviewUrlView> issue(UUID actorId, UUID attachmentId) {
        return attachments.findById(attachmentId).filter(a -> a.deletedAt() == null)
            .flatMap(attachment -> resources.findByIdAndOwnerId(attachment.resourceId(), actorId).thenReturn(attachment))
            .switchIfEmpty(Mono.error(new NotFoundException("Attachment 不存在或无权访问")))
            .flatMap(attachment -> blobs.findById(attachment.blobId())
                .switchIfEmpty(Mono.error(new NotFoundException("Attachment 对应的 Blob 不存在")))
                .flatMap(blob -> preferDeliveryBinding(actorId, attachmentId, blob)
                    .switchIfEmpty(Mono.defer(() -> fallbackToStorage(blob)))
                    .switchIfEmpty(Mono.error(new StorageUnavailableException("附件没有可用存储 Placement")))));
    }

    private Mono<AttachmentPreviewUrlView> preferDeliveryBinding(UUID actorId, UUID attachmentId, BlobEntity blob) {
        return resolveBinding(blob)
            .flatMap(binding -> issueBindingUrl(actorId, attachmentId, binding));
    }

    private Mono<MediaDeliveryBindingEntity> resolveBinding(BlobEntity blob) {
        return placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
            .filter(placement -> placement.placementState() == PlacementState.ACTIVE)
            .concatMap(placement -> providers.getByKey(placement.provider())
                .filter(provider -> provider.status() != StorageProviderStatus.DISABLED
                    && provider.status() != StorageProviderStatus.FAILED)
                .flatMap(provider -> bindings.findAllByStorageProviderIdOrderByPriorityAsc(provider.id())
                    .filter(MediaDeliveryBindingEntity::enabled)
                    .concatMap(binding -> deliveryProviders.findByProviderKey(binding.deliveryProviderKey())
                        .filter(deliveryProvider -> deliveryProvider.enabled()
                            && deliveryProvider.healthStatus() != DeliveryProviderHealthStatus.UNHEALTHY)
                        .map(ignored -> binding))
                    .next()))
            .next();
    }

    private Mono<AttachmentPreviewUrlView> issueBindingUrl(UUID actorId, UUID attachmentId,
                                                             MediaDeliveryBindingEntity binding) {
        DeliveryGrantRequest request = new DeliveryGrantRequest(900, null, null, DeliveryIntent.PLAYBACK, null, true);
        return deliveryGrants.issue(actorId, attachmentId, request)
            .flatMap(grant -> deliveryLeases.create(actorId, attachmentId,
                    new DeliveryLeaseRequest(grant.token(), request.ttlSeconds()))
                .flatMap(lease -> deliveryContracts.contract(attachmentId, grant, lease)
                    .filter(contract -> contract.deliveryProviderId() != null
                        && contract.rangeSupported() == (binding.rangePolicy() != DeliveryBindingRangePolicy.UNSUPPORTED))
                    .switchIfEmpty(Mono.error(new StorageUnavailableException("Delivery Binding 合同不可用"))))
                .map(contract -> new AttachmentPreviewUrlView(contract.method(), contract.url(), contract.expiresAt(),
                    contract.rangeSupported(), contract.contentType()))
                .onErrorResume(error -> deliveryGrants.revoke(actorId, grant.id())
                    .onErrorResume(revokeError -> Mono.empty())
                    .then(Mono.error(error))));
    }

    private Mono<AttachmentPreviewUrlView> fallbackToStorage(BlobEntity blob) {
        return placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                    .filter(placement -> placement.placementState() == PlacementState.ACTIVE)
                    .concatMap(placement -> providers.getByKey(placement.provider())
                        .flatMap(provider -> objects.createReadIntent(provider, placement.objectKey())
                            .map(intent -> new AttachmentPreviewUrlView(intent.method(), intent.url(), intent.expiresAt(), true,
                                blob.mediaType()))))
                    .next();
    }
}
