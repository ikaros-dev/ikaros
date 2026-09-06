package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Mono;
import run.ikaros.event.DurableEventService;

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
    void updatesProviderConfigurationAndPublishesChangedEvent() {
        DurableEventService events = mock(DurableEventService.class);
        when(events.append(any(), eq(1), eq("storage_provider"), any(UUID.class), any())).thenReturn(Mono.empty());
        InMemoryStorageProviderRegistry registry = new InMemoryStorageProviderRegistry(events);
        StorageProvider provider = registry.register("local", "filesystem", StorageTier.HOT,
            "secret://storage/local", Map.of()).block();

        StorageProvider updated = registry.update(provider.id(), new UpdateStorageProviderRequest(
            "s3", StorageTier.COLD, null, Map.of("bucket", "archive"))).block();

        assertEquals("s3", updated.providerType());
        assertEquals(StorageTier.COLD, updated.tier());
        assertEquals("archive", updated.metadata().get("bucket"));
        verify(events).append(eq("storage.provider.updated"), eq(1), eq("storage_provider"), eq(provider.id()), any());
    }
}
