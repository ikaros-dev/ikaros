package run.ikaros.storage;

import reactor.core.publisher.Mono;

public interface StorageProviderLifecycleInspector {
    boolean supports(StorageProvider provider);
    Mono<StorageProviderLifecycleState> inspect(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob);
}
