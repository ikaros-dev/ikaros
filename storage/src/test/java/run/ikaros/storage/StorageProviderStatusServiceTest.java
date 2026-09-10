package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.storage.api.StorageProviderProbeResult;
import run.ikaros.storage.api.StorageProviderProbeStatus;
import run.ikaros.storage.api.StorageTier;

class StorageProviderStatusServiceTest {
    @Test
    void returnsHealthAndOptionalCapacityWithoutInventingMissingValues() {
        UUID id = UUID.randomUUID();
        StorageProvider provider = new StorageProvider(id, "media", "S3", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://media", Map.of("capacity_bytes", 1000), Instant.now(), Instant.now());
        StorageProviderRegistry registry = mock(StorageProviderRegistry.class);
        StorageProviderProbeService probe = mock(StorageProviderProbeService.class);
        when(registry.get(id)).thenReturn(Mono.just(provider));
        when(probe.probe(id)).thenReturn(Mono.just(new StorageProviderProbeResult(id, StorageProviderProbeStatus.HEALTHY,
            true, true, true, Instant.now(), null)));

        StepVerifier.create(new StorageProviderStatusService(registry, probe).get(id))
            .assertNext(view -> {
                assertThat(view.providerStatus()).isEqualTo("ENABLED");
                assertThat(view.capacityBytes()).isEqualTo(1000L);
                assertThat(view.usedBytes()).isNull();
                assertThat(view.health().status()).isEqualTo(StorageProviderProbeStatus.HEALTHY);
            }).verifyComplete();
    }

    @Test
    void propagatesMissingProvider() {
        UUID id = UUID.randomUUID();
        StorageProviderRegistry registry = mock(StorageProviderRegistry.class);
        when(registry.get(id)).thenReturn(Mono.error(new run.ikaros.common.NotFoundException("Storage Provider 不存在")));
        StepVerifier.create(new StorageProviderStatusService(registry, mock(StorageProviderProbeService.class)).get(id))
            .expectError(run.ikaros.common.NotFoundException.class).verify();
    }
}
