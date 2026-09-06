package run.ikaros.storage;

import org.springframework.stereotype.Component;

/** 腾讯云 COS 的 S3 协议 adapter。 */
@Component
public class TencentCosS3StorageObjectProvider extends AbstractS3StorageObjectProvider {
    @Override public boolean supports(StorageProvider provider) {
        return "TENCENT_COS_S3".equalsIgnoreCase(provider.providerType());
    }
}
