package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.r2dbc.postgresql.codec.Json;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

class DeliveryGrantContractServiceTest {
    private final BlobRepository blobs = mock(BlobRepository.class);
    private final MediaDeliveryBindingRepository bindings = mock(MediaDeliveryBindingRepository.class);
    private final DeliveryProviderRepository providers = mock(DeliveryProviderRepository.class);
    private final StorageProviderRegistry storageProviders = mock(StorageProviderRegistry.class);
    private final BlobPlacementRepository placements = mock(BlobPlacementRepository.class);
    private final StorageObjectProviderRegistry storageObjects = mock(StorageObjectProviderRegistry.class);
    private final DeliveryGrantContractService service = new DeliveryGrantContractService(blobs, bindings, providers,
        storageProviders, placements, storageObjects, new ObjectMapper());

    @Test
    void cdnSignsStorageObjectWithConfiguredCdnEndpoint() {
        UUID attachmentId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        UUID storageProviderId = UUID.randomUUID();
        UUID deliveryProviderId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(900);
        MediaDeliveryBindingEntity binding = new MediaDeliveryBindingEntity(UUID.randomUUID(), storageProviderId, "cdn",
            1, true, DeliveryBindingCacheKeyPolicy.CONTENT_IDENTITY, DeliveryBindingRangePolicy.PASSTHROUGH, true,
            Instant.now(), Instant.now(), 0L);
        DeliveryProviderEntity deliveryProvider = new DeliveryProviderEntity(deliveryProviderId, "cdn",
            DeliveryProviderType.CDN, "CDN", null, Json.of("{\"endpoint\":\"https://media.example.com/\"}"),
            Json.of("{}"), DeliveryGrantRevocationLevel.IMMEDIATE, 1, DeliveryProviderHealthStatus.HEALTHY, true,
            Instant.now(), Instant.now(), 0L, null);
        StorageProvider storageProvider = new StorageProvider(storageProviderId, "oss", "S3", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://oss", Map.of(), Instant.now(), Instant.now());
        BlobEntity blob = new BlobEntity(blobId, "sha256", 42L, "video/mp4", BlobAvailability.AVAILABLE,
            Instant.now(), 0L);
        BlobPlacementEntity placement = new BlobPlacementEntity(UUID.randomUUID(), blobId, "oss", StorageTier.HOT,
            "media/video.mp4", PlacementState.ACTIVE, Instant.now(), Instant.now(), 0L);
        DeliveryGrantView grant = new DeliveryGrantView(UUID.randomUUID(), attachmentId, "token", "GET", expiresAt,
            null, null, DeliveryGrantRevocationLevel.IMMEDIATE);
        DeliveryLeaseView lease = new DeliveryLeaseView(UUID.randomUUID(), attachmentId, blobId, expiresAt,
            Instant.now(), true, binding.id(), 1, Instant.now(), "PRIMARY", 0, null, null);

        when(bindings.findById(binding.id())).thenReturn(Mono.just(binding));
        when(providers.findByProviderKey("cdn")).thenReturn(Mono.just(deliveryProvider));
        when(blobs.findById(blobId)).thenReturn(Mono.just(blob));
        when(storageProviders.get(storageProviderId)).thenReturn(Mono.just(storageProvider));
        when(placements.findFirstByBlobIdAndProvider(blobId, "oss")).thenReturn(Mono.just(placement));
        when(storageObjects.createReadIntent(eq(storageProvider), eq(placement.objectKey()), any(URI.class)))
            .thenReturn(Mono.just(new StorageReadIntent("GET",
                "https://media.example.com/media/video.mp4?X-Amz-Signature=signed", expiresAt)));

        StepVerifier.create(service.contract(attachmentId, grant, lease))
            .assertNext(contract -> {
                assertThat(contract.url()).isEqualTo("https://media.example.com/media/video.mp4?X-Amz-Signature=signed");
                assertThat(contract.method()).isEqualTo("GET");
            })
            .verifyComplete();

        verify(storageObjects).createReadIntent(eq(storageProvider), eq(placement.objectKey()),
            eq(URI.create("https://media.example.com/")));
    }
}
