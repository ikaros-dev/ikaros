package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import run.ikaros.storage.api.StorageProviderStatusView;

public record StorageProviderStatusResponse(UUID provider_id, String provider_status,
    StorageProviderProbeView health, Long capacity_bytes, Long used_bytes, Instant checked_at) {
    static StorageProviderStatusResponse from(StorageProviderStatusView view) {
        return new StorageProviderStatusResponse(view.providerId(), view.providerStatus(),
            StorageProviderProbeView.from(view.health()), view.capacityBytes(), view.usedBytes(), view.checkedAt());
    }
}
