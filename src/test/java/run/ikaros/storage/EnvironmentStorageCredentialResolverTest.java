package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class EnvironmentStorageCredentialResolverTest {
    @Test
    void rejectsUnsupportedSecretReferenceWithoutLeakingCredentials() {
        StepVerifier.create(new EnvironmentStorageCredentialResolver(mock(StorageProviderRepository.class), mock(StorageCredentialCipher.class)).resolve("secret://password/vault/item/1"))
            .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("不支持的 Storage Provider secret reference"))
            .verify();
    }

    @Test
    void reEncryptsLegacyProviderCredentialsOnRead() {
        String key = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        StorageCredentialCipher oldCipher = new StorageCredentialCipher(key);
        StorageCredentialCipher cipher = new StorageCredentialCipher(
            "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=", "v1=" + key, "v2");
        StorageProviderRepository providers = mock(StorageProviderRepository.class);
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        StorageProviderEntity provider = new StorageProviderEntity(id, "oss", "aliyun-oss", "HOT", "ENABLED",
            "secret://provider/oss", "{}", oldCipher.encrypt("access"), oldCipher.encrypt("secret"), null, now, now);
        when(providers.findByProviderKey("oss")).thenReturn(Mono.just(provider));
        when(providers.save(org.mockito.ArgumentMatchers.any(StorageProviderEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new EnvironmentStorageCredentialResolver(providers, cipher)
                .resolve("secret://provider/oss"))
            .expectNextCount(1)
            .verifyComplete();

        verify(providers).save(org.mockito.ArgumentMatchers.argThat(saved ->
            saved.accessKeyIdCiphertext().startsWith("enc-v1:v2:")
                && saved.secretAccessKeyCiphertext().startsWith("enc-v1:v2:")));
    }
}
