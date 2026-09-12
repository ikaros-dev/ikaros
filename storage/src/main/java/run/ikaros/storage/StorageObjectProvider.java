package run.ikaros.storage;

import java.net.URI;
import reactor.core.publisher.Mono;
import run.ikaros.storage.api.StorageProviderProbeResult;

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

    /** 服务端受控写入派生内容；大文件不得通过此接口写入。 */
    default Mono<StorageObjectMetadata> write(StorageProvider provider, String objectKey,
                                              String mediaType, byte[] content) {
        return Mono.error(new UnsupportedOperationException("Storage Provider 不支持服务端写入"));
    }

    /** 删除仅属于临时上传会话的对象；业务 Attachment 不通过此能力删除。 */
    Mono<Void> deleteObject(StorageProvider provider, String objectKey);

    default Mono<StorageProviderProbeResult> probe(StorageProvider provider) {
        return Mono.error(new UnsupportedOperationException("Provider probe unsupported"));
    }

}
