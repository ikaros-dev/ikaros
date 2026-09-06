package run.ikaros.storage;

import reactor.core.publisher.Mono;

/** Storage Provider 的内容读取能力，不拥有 Attachment 业务权限。 */
public interface StorageContentReader {
    default boolean supports(StorageProvider provider) { return false; }

    Mono<StorageContent> read(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob,
                              String range);
}
