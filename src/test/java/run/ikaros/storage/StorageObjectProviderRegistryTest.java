package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class StorageObjectProviderRegistryTest {
    @Test
    void routesPhysicalObjectOperationsToMatchingProviderType() {
        StorageObjectProvider adapter = mock(StorageObjectProvider.class);
        StorageProvider provider = new StorageProvider(UUID.randomUUID(), "oss", "ALIYUN_OSS_S3", StorageTier.HOT,
            StorageProviderStatus.ENABLED, "secret://oss", Map.of("bucket", "media", "endpoint", "https://oss.example"),
            Instant.now(), Instant.now());
        StorageUploadIntent intent = new StorageUploadIntent("PUT", "https://upload.example", "a.bin", Instant.now());
        when(adapter.supports(provider)).thenReturn(true);
        when(adapter.createUploadIntent(provider, new StorageUploadRequest("a.bin", 1, "text/plain")))
            .thenReturn(Mono.just(intent));

        StorageObjectProviderRegistry registry = new StorageObjectProviderRegistry(List.of(adapter));

        StepVerifier.create(registry.createUploadIntent(provider, new StorageUploadRequest("a.bin", 1, "text/plain")))
            .assertNext(value -> assertThat(value.url()).isEqualTo("https://upload.example"))
            .verifyComplete();
    }
}
