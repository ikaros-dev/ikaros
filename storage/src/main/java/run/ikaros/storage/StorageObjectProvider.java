package run.ikaros.storage;

import java.net.URI;
import reactor.core.publisher.Mono;

/**
 * 物理对象操作的统一 seam。业务层只依赖上传地址生成和对象完整性确认。
 */
public interface StorageObjectProvider {
    boolean supports(StorageProvider provider);

    Mono<StorageUploadIntent> createUploadIntent(StorageProvider provider, StorageUploadRequest request);

    Mono<StorageReadIntent> createReadIntent(StorageProvider provider, String objectKey);

    /**
     * Creates a read URL while signing the request for an alternate public endpoint.
     * The storage provider still supplies the bucket and credentials.
     */
    default Mono<StorageReadIntent> createReadIntent(StorageProvider provider, String objectKey,
                                                     URI signingEndpoint) {
        return createReadIntent(provider, objectKey);
    }

    Mono<StorageObjectMetadata> verify(StorageProvider provider, String objectKey);

}
