package run.ikaros.storage;

import reactor.core.publisher.Mono;

/** Provider-specific copy operation used to materialize a promoted Placement. */
public interface StorageContentCopier {
    boolean supports(StorageProvider provider);
    Mono<String> copy(StorageProvider provider, BlobPlacementEntity source, BlobEntity blob, StorageTier targetTier);
}
