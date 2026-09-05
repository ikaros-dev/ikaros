package run.ikaros.storage;

import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;

/** 将持久化 Provider 配置路由到唯一匹配的物理对象 adapter。 */
@Component
public class StorageObjectProviderRegistry {
    private final List<StorageObjectProvider> providers;

    public StorageObjectProviderRegistry(List<StorageObjectProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public Mono<StorageUploadIntent> createUploadIntent(StorageProvider provider, StorageUploadRequest request) {
        return find(provider).createUploadIntent(provider, request);
    }

    public Mono<StorageReadIntent> createReadIntent(StorageProvider provider, String objectKey) {
        return find(provider).createReadIntent(provider, objectKey);
    }

    public Mono<StorageReadIntent> createReadIntent(StorageProvider provider, String objectKey,
                                                    URI signingEndpoint) {
        return find(provider).createReadIntent(provider, objectKey, signingEndpoint);
    }

    public Mono<StorageObjectMetadata> verify(StorageProvider provider, String objectKey) {
        return find(provider).verify(provider, objectKey);
    }

    private StorageObjectProvider find(StorageProvider provider) {
        return providers.stream().filter(candidate -> candidate.supports(provider)).findFirst()
            .orElseThrow(() -> new ConflictException("未配置 Storage Provider 物理 adapter: " + provider.providerType()));
    }
}
