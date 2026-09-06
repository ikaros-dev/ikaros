package run.ikaros.storage;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record StorageProvider(UUID id, String providerKey, String providerType, StorageTier tier,
                              StorageProviderStatus status, String secretReference,
                              Map<String, Object> metadata, Instant createdAt, Instant updatedAt) {
    public StorageProvider {
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
    }
}
