package run.ikaros.storage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.storage.api.StorageTier;

class PersistentStorageProviderRegistryTest {
    @Test
    void registersProviderAndStoresOnlySecretReference() {
        StorageProviderRepository repository = mock(StorageProviderRepository.class);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        StorageCredentialCipher cipher = mock(StorageCredentialCipher.class);
        UUID id = UUID.randomUUID();
        when(repository.findByProviderKey("local")).thenReturn(reactor.core.publisher.Mono.empty());
        when(repository.save(any())).thenReturn(reactor.core.publisher.Mono.just(new StorageProviderEntity(id, "local", "filesystem", "HOT", "ENABLED", "secret://storage/local", Json.of("{}"), null, null, null, Instant.now(), Instant.now())));
        when(events.append(any(EventAppendRequest.class))).thenReturn(reactor.core.publisher.Mono.empty());
        PersistentStorageProviderRegistry registry = new PersistentStorageProviderRegistry(repository, new ObjectMapper(), events, cipher);

        StepVerifier.create(registry.register("local", "filesystem", StorageTier.HOT, "secret://storage/local", Map.of()))
            .expectNextMatches(provider -> provider.id().equals(id) && provider.secretReference().equals("secret://storage/local"))
            .verifyComplete();
    }

    @Test
    void insertsNewProviderWithNullVersionSoSpringDataPerformsInsert() {
        StorageProviderRepository repository = mock(StorageProviderRepository.class);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        StorageCredentialCipher cipher = mock(StorageCredentialCipher.class);
        java.util.concurrent.atomic.AtomicReference<StorageProviderEntity> persisted =
            new java.util.concurrent.atomic.AtomicReference<>();
        UUID id = UUID.randomUUID();
        when(repository.findByIdempotencyKey(any())).thenReturn(reactor.core.publisher.Mono.empty());
        when(repository.findByProviderKey("local")).thenReturn(reactor.core.publisher.Mono.empty());
        when(repository.save(any())).thenAnswer(invocation -> {
            persisted.set(invocation.getArgument(0));
            return reactor.core.publisher.Mono.just(new StorageProviderEntity(id, "local", "filesystem", "HOT",
                "ENABLED", "secret://storage/local", Json.of("{}"), null, null, null, Instant.now(), Instant.now()));
        });
        when(events.append(any(EventAppendRequest.class))).thenReturn(reactor.core.publisher.Mono.empty());
        PersistentStorageProviderRegistry registry = new PersistentStorageProviderRegistry(repository, new ObjectMapper(), events, cipher);

        StepVerifier.create(registry.registerConfigured("local", "filesystem", "Local", StorageTier.HOT,
                "secret://storage/local", Map.of(), Map.of(), "create-key-1", "a".repeat(64)))
            .expectNextCount(1).verifyComplete();

        org.junit.jupiter.api.Assertions.assertNull(persisted.get().version());
        org.junit.jupiter.api.Assertions.assertNull(persisted.get().id());
    }

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
