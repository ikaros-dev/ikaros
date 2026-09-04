package run.ikaros.storage;

import reactor.core.publisher.Mono;

/**
 * 物理对象操作的统一 seam。业务层只依赖上传地址生成和对象完整性确认。
 */
public interface StorageObjectProvider {
    boolean supports(StorageProvider provider);

    Mono<StorageUploadIntent> createUploadIntent(StorageProvider provider, StorageUploadRequest request);

    Mono<StorageObjectMetadata> verify(StorageProvider provider, String objectKey);
}
