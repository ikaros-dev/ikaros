package run.ikaros.storage;

import org.springframework.stereotype.Component;

/** 阿里云 OSS 的 S3 协议 adapter。 */
@Component
public class AliyunOssS3StorageObjectProvider extends AbstractS3StorageObjectProvider {
    @Override public boolean supports(StorageProvider provider) {
        return "ALIYUN_OSS_S3".equalsIgnoreCase(provider.providerType());
    }
}
