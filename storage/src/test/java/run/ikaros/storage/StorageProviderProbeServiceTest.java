package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.storage.api.StorageProviderProbeResult;
import run.ikaros.storage.api.StorageProviderProbeStatus;
import run.ikaros.storage.api.StorageTier;

class StorageProviderProbeServiceTest {
    @Test
    void returnsProbeResultForExistingProvider() {
        UUID id = UUID.randomUUID();
        StorageProvider provider = new StorageProvider(id, "media", "S3", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://media", Map.of(), Instant.now(), Instant.now());
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        StorageProviderProbeResult expected = new StorageProviderProbeResult(id, StorageProviderProbeStatus.HEALTHY,
            true, true, true, Instant.now(), null);
        when(providers.get(id)).thenReturn(Mono.just(provider));
        when(objects.probe(provider)).thenReturn(Mono.just(expected));

        StepVerifier.create(new StorageProviderProbeService(providers, objects).probe(id))
            .assertNext(result -> assertThat(result).isEqualTo(expected)).verifyComplete();
    }

    @Test
    void reportsUnsupportedAdapterWithoutWritingState() {
        UUID id = UUID.randomUUID();
        StorageProvider provider = new StorageProvider(id, "local", "LOCAL_FILESYSTEM", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://local", Map.of(), Instant.now(), Instant.now());
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        when(providers.get(id)).thenReturn(Mono.just(provider));
        when(objects.probe(provider)).thenReturn(Mono.error(new UnsupportedOperationException()));

        StepVerifier.create(new StorageProviderProbeService(providers, objects).probe(id))
            .assertNext(result -> assertThat(result.status()).isEqualTo(StorageProviderProbeStatus.UNSUPPORTED))
            .verifyComplete();
    }

    @Test
    void propagatesMissingProvider() {
        UUID id = UUID.randomUUID();
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        when(providers.get(id)).thenReturn(Mono.error(new run.ikaros.common.NotFoundException("Storage Provider 不存在")));

        StepVerifier.create(new StorageProviderProbeService(providers, mock(StorageObjectProviderRegistry.class)).probe(id))
            .expectError(run.ikaros.common.NotFoundException.class).verify();
        verify(providers).get(id);
    }
}
