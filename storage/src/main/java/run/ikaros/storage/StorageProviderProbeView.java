package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import run.ikaros.storage.api.StorageProviderProbeResult;

public record StorageProviderProbeView(UUID provider_id, String status, boolean connection, boolean read,
                                       boolean write, Instant checked_at, String error_code) {
    static StorageProviderProbeView from(StorageProviderProbeResult result) {
        return new StorageProviderProbeView(result.providerId(), result.status().name(), result.connection(),
            result.read(), result.write(), result.checkedAt(), result.errorCode());
    }
}
