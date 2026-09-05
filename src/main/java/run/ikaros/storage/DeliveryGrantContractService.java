package run.ikaros.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DeliveryGrantContractService {
    private final BlobRepository blobs;
    private final MediaDeliveryBindingRepository bindings;
    private final DeliveryProviderRepository providers;
    private final StorageProviderRegistry storageProviders;
    private final BlobPlacementRepository placements;
    private final StorageObjectProviderRegistry storageObjects;

    public DeliveryGrantContractService(BlobRepository blobs, MediaDeliveryBindingRepository bindings,
                                        DeliveryProviderRepository providers, StorageProviderRegistry storageProviders,
                                        BlobPlacementRepository placements, StorageObjectProviderRegistry storageObjects) {
        this.blobs = blobs;
        this.bindings = bindings;
        this.providers = providers;
        this.storageProviders = storageProviders;
        this.placements = placements;
        this.storageObjects = storageObjects;
    }

    public Mono<DeliveryGrantContractView> contract(UUID attachmentId, DeliveryGrantView grant,
                                                     DeliveryLeaseView lease) {
        return bindings.findById(lease.bindingId())
            .flatMap(binding -> providers.findByProviderKey(binding.deliveryProviderKey())
                .zipWith(blobs.findById(lease.blobId()))
                .flatMap(providerAndBlob -> {
                    DeliveryProviderEntity deliveryProvider = providerAndBlob.getT1();
                    BlobEntity blob = providerAndBlob.getT2();
                    if (binding.originType() != DeliveryBindingOriginType.STORAGE_PROVIDER) {
                        return Mono.just(new DeliveryGrantContractView(grant.id(), grant.attachmentId(), lease.id(),
                            deliveryProvider.id(), grant.method(), deliveryUrl(deliveryProvider, attachmentId, grant.token()), grant.expiresAt(),
                            binding.rangePolicy() != DeliveryBindingRangePolicy.UNSUPPORTED,
                            blob.mediaType(), blob.sizeBytes(), grant.revocationLevel()));
                    }
                    return directReadContract(attachmentId, lease, grant, binding, deliveryProvider, blob);
                }));
    }

    private Mono<DeliveryGrantContractView> directReadContract(UUID attachmentId, DeliveryLeaseView lease,
                                                                 DeliveryGrantView grant, MediaDeliveryBindingEntity binding,
                                                                 DeliveryProviderEntity deliveryProvider, BlobEntity blob) {
        return storageProviders.get(binding.storageProviderId())
            .flatMap(storageProvider -> placements.findFirstByBlobIdAndProvider(lease.blobId(), storageProvider.providerKey())
                .flatMap(placement -> storageObjects.createReadIntent(storageProvider, placement.objectKey())))
            .map(read -> new DeliveryGrantContractView(grant.id(), grant.attachmentId(), lease.id(),
                deliveryProvider.id(), read.method(), read.url(), read.expiresAt(),
                binding.rangePolicy() != DeliveryBindingRangePolicy.UNSUPPORTED,
                blob.mediaType(), blob.sizeBytes(), grant.revocationLevel()));
    }

    private String deliveryUrl(DeliveryProviderEntity provider, UUID attachmentId, String token) {
        // CDN is a cache/proxy layer, not a URL signer. Without an explicit
        // route contract, its endpoint cannot safely be combined with this
        // origin API path. Keep the Ikaros grant URL until such a route exists.
        return "/api/attachments/" + attachmentId + "/content?delivery_grant=" + token;
    }
}
