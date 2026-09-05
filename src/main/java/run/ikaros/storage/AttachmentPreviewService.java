package run.ikaros.storage;

import java.util.UUID;
import java.util.Comparator;
import java.util.List;
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
    private final MediaDeliveryBindingRepository bindings;
    private final DeliveryProviderRepository deliveryProviders;
    private final DeliveryGrantService deliveryGrants;
    private final DeliveryLeaseService deliveryLeases;
    private final DeliveryGrantContractService deliveryContracts;

    public AttachmentPreviewService(AttachmentRepository attachments, ResourceRepository resources, BlobRepository blobs,
                                     BlobPlacementRepository placements, StorageProviderRegistry providers,
                                     MediaDeliveryBindingRepository bindings,
                                     DeliveryProviderRepository deliveryProviders, DeliveryGrantService deliveryGrants,
                                     DeliveryLeaseService deliveryLeases, DeliveryGrantContractService deliveryContracts) {
        this.attachments = attachments; this.resources = resources; this.blobs = blobs; this.placements = placements;
        this.providers = providers; this.bindings = bindings; this.deliveryProviders = deliveryProviders;
        this.deliveryGrants = deliveryGrants;
        this.deliveryLeases = deliveryLeases; this.deliveryContracts = deliveryContracts;
    }

    public Mono<AttachmentPreviewUrlView> issue(UUID actorId, UUID attachmentId) {
        return issue(actorId, attachmentId, null);
    }

    public Mono<AttachmentPreviewUrlView> issue(UUID actorId, UUID attachmentId, String requestedProviderKey) {
        return attachments.findById(attachmentId).filter(a -> a.deletedAt() == null)
            .flatMap(attachment -> resources.findByIdAndOwnerId(attachment.resourceId(), actorId).thenReturn(attachment))
            .switchIfEmpty(Mono.error(new NotFoundException("Attachment 不存在或无权访问")))
            .flatMap(attachment -> blobs.findById(attachment.blobId())
                .switchIfEmpty(Mono.error(new NotFoundException("Attachment 对应的 Blob 不存在")))
                .flatMap(blob -> preferDeliveryBinding(actorId, attachmentId, blob, requestedProviderKey)
                    .switchIfEmpty(Mono.error(new StorageUnavailableException("附件没有可用 Delivery Binding")))));
    }

    private Mono<AttachmentPreviewUrlView> preferDeliveryBinding(UUID actorId, UUID attachmentId, BlobEntity blob,
                                                                  String requestedProviderKey) {
        return resolveBindings(blob).collectList()
            .flatMap(candidates -> {
                if (candidates.isEmpty()) return Mono.error(new StorageUnavailableException("附件没有可用 Delivery Binding"));
                DeliveryCandidate selected = candidates.stream()
                    .filter(candidate -> requestedProviderKey != null && !requestedProviderKey.isBlank()
                        && candidate.provider().providerKey().equals(requestedProviderKey.trim()))
                    .findFirst()
                    .orElseGet(() -> candidates.stream()
                        .min(Comparator.comparingInt(candidate -> candidate.binding().priority())).orElseThrow());
                List<AttachmentDeliveryProviderOptionView> options = candidates.stream()
                    .sorted(Comparator.comparingInt(candidate -> candidate.binding().priority()))
                    .map(candidate -> option(candidate, candidate == selected)).toList();
                return issueBindingUrl(actorId, attachmentId, selected.binding())
                    .map(url -> new AttachmentPreviewUrlView(url.method(), url.url(), url.expiresAt(), url.rangeSupported(),
                        url.contentType(), option(selected, true), options));
            });
    }

    private reactor.core.publisher.Flux<DeliveryCandidate> resolveBindings(BlobEntity blob) {
        return placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
            .filter(placement -> placement.placementState() == PlacementState.ACTIVE)
            .concatMap(placement -> providers.getByKey(placement.provider())
                .filter(provider -> provider.status() != StorageProviderStatus.DISABLED
                    && provider.status() != StorageProviderStatus.FAILED)
                .flatMapMany(provider -> bindings.findAllByStorageProviderIdOrderByPriorityAsc(provider.id())
                    .filter(MediaDeliveryBindingEntity::enabled)
                    .concatMap(binding -> deliveryProviders.findByProviderKey(binding.deliveryProviderKey())
                        .filter(deliveryProvider -> deliveryProvider.enabled()
                            && deliveryProvider.healthStatus() != DeliveryProviderHealthStatus.UNHEALTHY)
                        .map(deliveryProvider -> new DeliveryCandidate(binding, deliveryProvider)))));
    }

    private AttachmentDeliveryProviderOptionView option(DeliveryCandidate candidate, boolean selected) {
        DeliveryProviderEntity provider = candidate.provider();
        return new AttachmentDeliveryProviderOptionView(candidate.binding().id(), provider.id(), provider.providerKey(),
            provider.displayName(), provider.providerType(), candidate.binding().priority(), selected);
    }

    private Mono<AttachmentPreviewUrlView> issueBindingUrl(UUID actorId, UUID attachmentId,
                                                             MediaDeliveryBindingEntity binding) {
        DeliveryGrantRequest request = new DeliveryGrantRequest(900, null, null, DeliveryIntent.PLAYBACK, null, true);
        return deliveryGrants.issue(actorId, attachmentId, request)
            .flatMap(grant -> deliveryLeases.create(actorId, attachmentId,
                    new DeliveryLeaseRequest(grant.token(), request.ttlSeconds()), binding.id())
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

    private record DeliveryCandidate(MediaDeliveryBindingEntity binding, DeliveryProviderEntity provider) { }
}
