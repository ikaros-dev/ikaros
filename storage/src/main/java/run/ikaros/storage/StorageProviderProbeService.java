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

    /** 显式探测入口：停用的 Provider 必须先启用。 */
    public Mono<StorageProviderProbeResult> probe(UUID providerId) {
        return providers.get(providerId).flatMap(provider -> provider.enabled()
            ? probeProvider(provider)
            : Mono.error(new ConflictException("请先启用 Storage Provider 再探测")));
    }

    /** 作为其他操作一部分的探测，不校验启用状态。 */
    Mono<StorageProviderProbeResult> probeProvider(UUID providerId) {
        return providers.get(providerId).flatMap(this::probeProvider);
    }

    Mono<StorageProviderProbeResult> probeProvider(StorageProvider provider) {
        return objects.probe(provider)
            .onErrorResume(UnsupportedOperationException.class, error -> Mono.just(new StorageProviderProbeResult(
                provider.id(), StorageProviderProbeStatus.UNSUPPORTED, true, false, false, Instant.now(),
                "UNSUPPORTED_OPERATION")))
            .onErrorResume(ConflictException.class, error -> Mono.error(error));
    }
}
