package run.ikaros.storage;

import reactor.core.publisher.Mono;

public interface StorageRestoreExecutor {
    boolean supports(StorageProvider provider);
    Mono<StorageRestoreResult> restore(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob);
}
