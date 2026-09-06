package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class StorageProviderListingTest {
    @Test
    void unpagedProviderListIsBounded() {
        InMemoryStorageProviderRegistry registry = new InMemoryStorageProviderRegistry();
        for (int i = 0; i < 101; i++) {
            registry.register("provider-" + i, "filesystem", StorageTier.HOT, "secret://provider-" + i, Map.of()).block();
        }
        assertEquals(100L, registry.list().count().block());
    }
}
