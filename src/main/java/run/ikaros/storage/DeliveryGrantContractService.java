package run.ikaros.storage;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class DeliveryGrantContractService {
    private final BlobRepository blobs;
    private final MediaDeliveryBindingRepository bindings;
    private final DeliveryProviderRepository providers;
    private final StorageProviderRegistry storageProviders;
    private final BlobPlacementRepository placements;
    private final StorageObjectProviderRegistry storageObjects;
    private final ObjectMapper mapper;

    public DeliveryGrantContractService(BlobRepository blobs, MediaDeliveryBindingRepository bindings,
                                        DeliveryProviderRepository providers, StorageProviderRegistry storageProviders,
                                        BlobPlacementRepository placements, StorageObjectProviderRegistry storageObjects,
                                        ObjectMapper mapper) {
        this.blobs = blobs;
        this.bindings = bindings;
        this.providers = providers;
        this.storageProviders = storageProviders;
        this.placements = placements;
        this.storageObjects = storageObjects;
        this.mapper = mapper;
    }

    public Mono<DeliveryGrantContractView> contract(UUID attachmentId, DeliveryGrantView grant,
                                                     DeliveryLeaseView lease) {
        return bindings.findById(lease.bindingId())
            .flatMap(binding -> providers.findByProviderKey(binding.deliveryProviderKey())
                .zipWith(blobs.findById(lease.blobId()))
                .flatMap(providerAndBlob -> {
                    DeliveryProviderEntity deliveryProvider = providerAndBlob.getT1();
                    BlobEntity blob = providerAndBlob.getT2();
                    if (deliveryProvider.providerType() != DeliveryProviderType.DIRECT) {
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
        String localPath = "/api/attachments/" + attachmentId + "/content?delivery_grant=" + token;
        if (provider.providerType() == DeliveryProviderType.SERVER_PROXY) return localPath;
        try {
            Map<String, Object> config = mapper.readValue(provider.config() == null ? "{}" : provider.config().asString(),
                new TypeReference<>() { });
            Object configuredEndpoint = config.get("endpoint");
            if (configuredEndpoint == null || configuredEndpoint.toString().isBlank()) return localPath;
            URI endpoint = URI.create(configuredEndpoint.toString().trim());
            if (endpoint.getScheme() == null || endpoint.getHost() == null) return localPath;
            String base = endpoint.toString().replaceAll("/+\\z", "");
            return base + localPath;
        } catch (IllegalArgumentException | JacksonException ignored) {
            return localPath;
        }
    }
}
