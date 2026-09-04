package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class StorageProviderCredentialRotationServiceTest {
    @Test
    void rotatesAllConfiguredCredentialFieldsWithoutReturningPlaintext() {
        String oldKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        String newKey = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=";
        StorageCredentialCipher oldCipher = new StorageCredentialCipher(oldKey);
        StorageCredentialCipher cipher = new StorageCredentialCipher(newKey, "v1=" + oldKey, "v2");
        StorageProviderRepository providers = mock(StorageProviderRepository.class);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        StorageProviderEntity provider = new StorageProviderEntity(id, "oss", "aliyun-oss", "HOT", "ENABLED",
            "secret://provider/oss", "{}", oldCipher.encrypt("access"), oldCipher.encrypt("secret"),
            oldCipher.encrypt("session"), now, now);
        when(providers.findById(id)).thenReturn(Mono.just(provider));
        when(providers.save(any(StorageProviderEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StorageCredentialRotationView result = new StorageProviderCredentialRotationService(providers, cipher).rotate(id).block();

        assertThat(result.rotatedFields()).isEqualTo(3);
        assertThat(result.encryptionKeyVersion()).isEqualTo("v2");
        verify(providers).save(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.accessKeyIdCiphertext().startsWith("enc-v1:v2:")
                && saved.secretAccessKeyCiphertext().startsWith("enc-v1:v2:")
                && saved.sessionTokenCiphertext().startsWith("enc-v1:v2:")));
    }
}
