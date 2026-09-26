package run.ikaros.storage;

import run.ikaros.storage.api.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record StorageProvider(UUID id, String providerKey, String providerType, StorageTier tier,
                              StorageProviderStatus status, String secretReference,
                              Map<String, Object> metadata, Instant createdAt, Instant updatedAt,
                              String displayName, Map<String, Object> capabilities, boolean enabled,
                              String drainStatus, long version) {
    public StorageProvider {
        metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
        capabilities = Map.copyOf(capabilities == null ? Map.of() : capabilities);
    }

    public StorageProvider(UUID id, String providerKey, String providerType, StorageTier tier,
                           StorageProviderStatus status, String secretReference,
                           Map<String, Object> metadata, Instant createdAt, Instant updatedAt) {
        this(id, providerKey, providerType, tier, status, secretReference, metadata, createdAt, updatedAt,
            providerKey, Map.of(), status == StorageProviderStatus.ENABLED,
            status == StorageProviderStatus.DRAINING ? "DRAINING" : "NORMAL", 0);
    }
}
