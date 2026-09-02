package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
}
