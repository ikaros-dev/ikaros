package run.ikaros.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class EnvironmentStorageCredentialResolverTest {
    @Test
    void rejectsUnsupportedSecretReferenceWithoutLeakingCredentials() {
        StepVerifier.create(new EnvironmentStorageCredentialResolver(mock(StorageProviderRepository.class), mock(StorageCredentialCipher.class)).resolve("secret://password/vault/item/1"))
            .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("不支持的 Storage Provider secret reference"))
            .verify();
    }
}
