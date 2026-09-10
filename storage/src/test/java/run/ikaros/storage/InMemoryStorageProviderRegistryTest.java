package run.ikaros.storage;

import run.ikaros.storage.api.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;

class InMemoryStorageProviderRegistryTest {
    @Test
    void disabledProviderCannotReceiveWrites() {
        InMemoryStorageProviderRegistry registry = new InMemoryStorageProviderRegistry();
        StorageProvider provider = registry.register("local", "filesystem", StorageTier.HOT,
            "secret://storage/local", Map.of()).block();
        registry.disable(provider.id()).block();
        assertThrows(RuntimeException.class, () -> registry.requireWritable(provider.id()).block());
        assertEquals(StorageProviderStatus.DISABLED, registry.get(provider.id()).block().status());
    }

    @Test
    void providerCanBeReenabledAfterDisableAndPublishesBothTransitions() {
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        when(events.append(any(EventAppendRequest.class))).thenReturn(Mono.empty());
        InMemoryStorageProviderRegistry registry = new InMemoryStorageProviderRegistry(events);
        StorageProvider provider = registry.register("local", "filesystem", StorageTier.HOT,
            "secret://storage/local", Map.of()).block();

        registry.disable(provider.id()).block();
        assertEquals(StorageProviderStatus.DISABLED, registry.get(provider.id()).block().status());
        registry.enable(provider.id()).block();
        assertEquals(StorageProviderStatus.ENABLED, registry.get(provider.id()).block().status());
        verify(events, org.mockito.Mockito.times(2)).append(any(EventAppendRequest.class));
    }

    @Test
    void updatesProviderConfigurationAndPublishesChangedEvent() {
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        when(events.append(any(EventAppendRequest.class))).thenReturn(Mono.empty());
        InMemoryStorageProviderRegistry registry = new InMemoryStorageProviderRegistry(events);
        StorageProvider provider = registry.register("local", "filesystem", StorageTier.HOT,
            "secret://storage/local", Map.of()).block();

        StorageProvider updated = registry.update(provider.id(), new UpdateStorageProviderRequest(
            "s3", StorageTier.COLD, null, Map.of("bucket", "archive"))).block();

        assertEquals("s3", updated.providerType());
        assertEquals(StorageTier.COLD, updated.tier());
        assertEquals("archive", updated.metadata().get("bucket"));
        verify(events).append(argThat(request -> request.eventType().equals("storage.provider.updated")
            && request.producerSubsystem().equals("storage") && request.subjectType().equals("storage_provider")
            && request.subjectId().equals(provider.id())));
    }
}
