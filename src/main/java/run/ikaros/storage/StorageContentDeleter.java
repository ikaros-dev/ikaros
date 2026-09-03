package run.ikaros.storage;

import reactor.core.publisher.Mono;

/** Storage Provider 的受控物理删除能力，仅由 GC/受控任务调用。 */
public interface StorageContentDeleter {
    boolean supports(StorageProvider provider);
    Mono<Void> delete(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob);
}
