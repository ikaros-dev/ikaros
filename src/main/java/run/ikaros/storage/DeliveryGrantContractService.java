package run.ikaros.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DeliveryGrantContractService {
    private final BlobRepository blobs;
    private final MediaDeliveryBindingRepository bindings;
    private final DeliveryProviderRepository providers;

    public DeliveryGrantContractService(BlobRepository blobs, MediaDeliveryBindingRepository bindings,
                                        DeliveryProviderRepository providers) {
        this.blobs = blobs;
        this.bindings = bindings;
        this.providers = providers;
    }

    public Mono<DeliveryGrantContractView> contract(UUID attachmentId, DeliveryGrantView grant,
                                                     DeliveryLeaseView lease) {
        return bindings.findById(lease.bindingId())
            .flatMap(binding -> providers.findByProviderKey(binding.deliveryProviderKey())
                .zipWith(blobs.findById(lease.blobId()))
                .map(providerAndBlob -> new DeliveryGrantContractView(grant.id(), grant.attachmentId(), lease.id(),
                    providerAndBlob.getT1().id(), grant.method(), "/api/attachments/" + attachmentId
                        + "/content?delivery_grant=" + grant.token(), grant.expiresAt(),
                    binding.rangePolicy() != DeliveryBindingRangePolicy.UNSUPPORTED,
                    providerAndBlob.getT2().mediaType(), providerAndBlob.getT2().sizeBytes(), grant.revocationLevel())));
    }
}
