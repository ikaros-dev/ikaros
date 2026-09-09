package run.ikaros.storage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import run.ikaros.storage.api.StorageTier;

class PersistentStorageProviderRegistryTest {
    @Test
    void rejectsPlaintextCredentialMetadataBeforePersistence() {
        StorageProviderRepository repository = mock(StorageProviderRepository.class);
        PersistentStorageProviderRegistry registry = new PersistentStorageProviderRegistry(repository,
            new ObjectMapper(), mock(run.ikaros.integration.api.DurableEventPublisher.class),
            mock(StorageCredentialCipher.class));

        StepVerifier.create(registry.register("local", "filesystem", StorageTier.HOT,
                "secret://storage/local", Map.of("access_password", "secret-value")))
            .expectErrorMessage("Provider metadata 不得保存明文凭据").verify();
        verifyNoInteractions(repository);
    }
}
