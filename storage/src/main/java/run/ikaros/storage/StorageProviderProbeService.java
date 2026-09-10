package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.storage.api.StorageProviderProbeResult;
import run.ikaros.storage.api.StorageProviderProbeStatus;

@Service
public class StorageProviderProbeService {
    private final StorageProviderRegistry providers;
    private final StorageObjectProviderRegistry objects;

    public StorageProviderProbeService(StorageProviderRegistry providers, StorageObjectProviderRegistry objects) {
        this.providers = providers;
        this.objects = objects;
    }

    public Mono<StorageProviderProbeResult> probe(UUID providerId) {
        return providers.get(providerId).flatMap(provider -> objects.probe(provider)
            .onErrorResume(UnsupportedOperationException.class, error -> Mono.just(new StorageProviderProbeResult(
                provider.id(), StorageProviderProbeStatus.UNSUPPORTED, true, false, false, Instant.now(),
                "UNSUPPORTED_OPERATION")))
            .onErrorResume(ConflictException.class, error -> Mono.error(error)));
    }
}
