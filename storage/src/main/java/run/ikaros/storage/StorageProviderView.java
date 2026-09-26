package run.ikaros.storage;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.util.Map;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StorageProviderView(UUID id, String providerKey, String providerType, String displayName,
                                  String tier, boolean enabled, String drainStatus,
                                  Map<String, Object> capabilities, long version,
                                  Map<String, Object> configuration) {
    public StorageProviderView {
        capabilities = Map.copyOf(capabilities == null ? Map.of() : capabilities);
        configuration = Map.copyOf(configuration == null ? Map.of() : configuration);
    }

    static StorageProviderView from(StorageProvider provider) {
        return new StorageProviderView(provider.id(), provider.providerKey(), provider.providerType(),
            provider.displayName(), provider.tier().name(), provider.enabled(), provider.drainStatus(),
            provider.capabilities(), provider.version(), provider.metadata());
    }
}
