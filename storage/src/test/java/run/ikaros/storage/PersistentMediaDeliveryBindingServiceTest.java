package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.storage.api.DeliveryBindingCacheKeyPolicy;
import run.ikaros.storage.api.DeliveryBindingRangePolicy;
import run.ikaros.storage.api.DeliveryProviderType;
import run.ikaros.storage.api.MediaDeliveryBindingRequest;
import run.ikaros.storage.api.StorageTier;

class PersistentMediaDeliveryBindingServiceTest {
    @Test
    void createsBindingForExistingStorageAndDeliveryProviders() {
        UUID storageId = UUID.randomUUID();
        StorageProvider storage = new StorageProvider(storageId, "origin", "S3", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://origin", Map.of(), Instant.now(), Instant.now());
        DeliveryProviderEntity delivery = provider("cdn");
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        DeliveryProviderRepository deliveryProviders = mock(DeliveryProviderRepository.class);
        MediaDeliveryBindingRepository bindings = mock(MediaDeliveryBindingRepository.class);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        when(providers.get(storageId)).thenReturn(Mono.just(storage));
        when(deliveryProviders.findByProviderKey("cdn")).thenReturn(Mono.just(delivery));
        when(bindings.findByStorageProviderIdAndDeliveryProviderKey(storageId, "cdn")).thenReturn(Mono.empty());
        when(bindings.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(events.append(any())).thenReturn(Mono.empty());

        var request = new MediaDeliveryBindingRequest("cdn", 1, true, DeliveryBindingCacheKeyPolicy.CONTENT_IDENTITY,
            DeliveryBindingRangePolicy.PASSTHROUGH, true);
        StepVerifier.create(new PersistentMediaDeliveryBindingService(providers, bindings, mock(MediaDeliveryLeaseRepository.class),
                deliveryProviders, events).create(storageId, request))
            .assertNext(view -> {
                assertThat(view.storageProviderId()).isEqualTo(storageId);
                assertThat(view.deliveryProviderKey()).isEqualTo("cdn");
                assertThat(view.priority()).isEqualTo(1);
            }).verifyComplete();
    }

    @Test
    void rejectsUnknownDeliveryProviderBeforeSavingBinding() {
        UUID storageId = UUID.randomUUID();
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        DeliveryProviderRepository deliveryProviders = mock(DeliveryProviderRepository.class);
        when(providers.get(storageId)).thenReturn(Mono.just(new StorageProvider(storageId, "origin", "S3", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://origin", Map.of(), Instant.now(), Instant.now())));
        when(deliveryProviders.findByProviderKey("missing")).thenReturn(Mono.empty());
        MediaDeliveryBindingRepository bindings = mock(MediaDeliveryBindingRepository.class);
        var request = new MediaDeliveryBindingRequest("missing", 0, true, DeliveryBindingCacheKeyPolicy.CONTENT_IDENTITY,
            DeliveryBindingRangePolicy.PASSTHROUGH, false);
        StepVerifier.create(new PersistentMediaDeliveryBindingService(providers, bindings, mock(MediaDeliveryLeaseRepository.class),
                deliveryProviders, mock(DurableEventPublisher.class)).create(storageId, request))
            .expectError(run.ikaros.common.NotFoundException.class).verify();
    }

    private DeliveryProviderEntity provider(String key) {
        Instant now = Instant.now();
        return new DeliveryProviderEntity(UUID.randomUUID(), key, DeliveryProviderType.DIRECT, "CDN", "secret://cdn",
            "{}", "{}", run.ikaros.storage.api.DeliveryGrantRevocationLevel.IMMEDIATE, 1,
            run.ikaros.storage.api.DeliveryProviderHealthStatus.UNKNOWN, true, now, now, 0L);
    }
}
