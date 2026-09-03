package run.ikaros.storage;

import reactor.core.publisher.Mono;

public interface StorageRestoreStatusQuery {
    boolean supports(StorageProvider provider);
    Mono<StorageRestoreProviderStatus> query(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob);
}
