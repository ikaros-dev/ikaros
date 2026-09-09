package run.ikaros.storage;

import run.ikaros.storage.api.StorageProviderProbeResult;
import reactor.core.publisher.Mono;

public interface StorageProviderObjectProbe {
    Mono<StorageProviderProbeResult> probe(StorageProvider provider);
}
