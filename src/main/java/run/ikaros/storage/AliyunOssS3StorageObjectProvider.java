package run.ikaros.storage;

import java.time.Duration;
import org.springframework.stereotype.Component;

/** 阿里云 OSS 的 S3 协议 adapter。 */
@Component
public class AliyunOssS3StorageObjectProvider extends AbstractS3StorageObjectProvider {
    public AliyunOssS3StorageObjectProvider(Duration timeout) { super(timeout); }

    @Override public boolean supports(StorageProvider provider) {
        return "ALIYUN_OSS_S3".equalsIgnoreCase(provider.providerType());
    }
}
