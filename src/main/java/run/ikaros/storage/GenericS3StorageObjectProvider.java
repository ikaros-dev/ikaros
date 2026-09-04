package run.ikaros.storage;

import java.time.Duration;
import org.springframework.stereotype.Component;

/** 通用 AWS S3 及其他 S3-compatible 服务的 adapter。 */
@Component
public class GenericS3StorageObjectProvider extends AbstractS3StorageObjectProvider {
    public GenericS3StorageObjectProvider(Duration timeout) { super(timeout); }

    @Override public boolean supports(StorageProvider provider) {
        return switch (provider.providerType().toUpperCase()) {
            case "S3", "AWS_S3", "S3_COMPATIBLE" -> true;
            default -> false;
        };
    }
}
