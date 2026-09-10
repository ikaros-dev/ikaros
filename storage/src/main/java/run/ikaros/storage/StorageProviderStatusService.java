package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.storage.api.StorageProviderStatusView;

@Service
public class StorageProviderStatusService {
    private final StorageProviderRegistry providers;
    private final StorageProviderProbeService probe;

    public StorageProviderStatusService(StorageProviderRegistry providers, StorageProviderProbeService probe) {
        this.providers = providers;
        this.probe = probe;
    }

    public Mono<StorageProviderStatusView> get(UUID providerId) {
        return providers.get(providerId).flatMap(provider -> probe.probe(providerId)
            .map(result -> new StorageProviderStatusView(provider.id(), provider.status().name(), result,
                number(provider.metadata().get("capacity_bytes")), number(provider.metadata().get("used_bytes")),
                Instant.now())));
    }

    private Long number(Object value) {
        if (value instanceof Number number && number.longValue() >= 0) return number.longValue();
        if (value instanceof String text) {
            try { long parsed = Long.parseLong(text); return parsed >= 0 ? parsed : null; }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }
}
