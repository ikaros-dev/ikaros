package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class StorageProviderCredentialServiceTest {
    @Test
    void replacesCredentialsUsingCurrentEncryptionKey() {
        StorageProviderRepository providers = mock(StorageProviderRepository.class);
        StorageCredentialCipher cipher = new StorageCredentialCipher("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        StorageProviderEntity provider = new StorageProviderEntity(id, "oss", "S3", "HOT", "ENABLED",
            "secret://provider/oss", "{\"bucket\":\"test\"}", "broken", "broken", null, now, now);
        when(providers.findById(id)).thenReturn(Mono.just(provider));
        when(providers.save(any(StorageProviderEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        new StorageProviderCredentialService(providers, cipher)
            .replace(id, new ReplaceStorageProviderCredentialsRequest("access", "secret", null)).block();

        var saved = org.mockito.ArgumentCaptor.forClass(StorageProviderEntity.class);
        org.mockito.Mockito.verify(providers).save(saved.capture());
        assertThat(cipher.decrypt(saved.getValue().accessKeyIdCiphertext())).isEqualTo("access");
        assertThat(cipher.decrypt(saved.getValue().secretAccessKeyCiphertext())).isEqualTo("secret");
        assertThat(saved.getValue().sessionTokenCiphertext()).isNull();
    }
}
