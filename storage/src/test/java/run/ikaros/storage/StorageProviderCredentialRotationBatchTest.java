package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class StorageProviderCredentialRotationBatchTest {
    @Test
    void rotatesProvidersSequentiallyAndReportsAggregate() {
        String oldKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        String newKey = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=";
        StorageCredentialCipher oldCipher = new StorageCredentialCipher(oldKey);
        StorageCredentialCipher cipher = new StorageCredentialCipher(newKey, "v1=" + oldKey, "v2");
        StorageProviderRepository providers = mock(StorageProviderRepository.class);
        Instant now = Instant.now();
        StorageProviderEntity first = provider(oldCipher, "oss", now);
        StorageProviderEntity second = provider(cipher, "cos", now);
        when(providers.findAll()).thenReturn(Flux.just(first, second));
        when(providers.save(any(StorageProviderEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StorageCredentialBatchRotationView result = new StorageProviderCredentialRotationService(providers, cipher).rotateAll().block();

        assertThat(result.encryptionKeyVersion()).isEqualTo("v2");
        assertThat(result.processedProviders()).isEqualTo(2);
        assertThat(result.rotatedProviders()).isEqualTo(1);
        assertThat(result.rotatedFields()).isEqualTo(2);
    }

    private StorageProviderEntity provider(StorageCredentialCipher cipher, String key, Instant now) {
        return new StorageProviderEntity(UUID.randomUUID(), key, "s3", "HOT", "ENABLED",
            "secret://provider/" + key, "{}", cipher.encrypt("access"), cipher.encrypt("secret"), null, now, now);
    }
}
