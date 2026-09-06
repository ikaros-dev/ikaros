package run.ikaros.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.common.ConflictException;
import run.ikaros.common.StorageUnavailableException;

@Component
public class LocalStorageContentDeleter implements StorageContentDeleter {
    @Override public boolean supports(StorageProvider provider) {
        return "LOCAL_FILESYSTEM".equalsIgnoreCase(provider.providerType());
    }

    @Override public Mono<Void> delete(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob) {
        Object rootValue = provider.metadata().get("rootPath");
        if (!(rootValue instanceof String rootText) || rootText.isBlank())
            return Mono.error(new StorageUnavailableException("本地 Storage Provider 缺少 rootPath 配置"));
        Path root = Path.of(rootText).toAbsolutePath().normalize();
        Path file = root.resolve(placement.objectKey()).normalize();
        if (!file.startsWith(root)) return Mono.error(new ConflictException("附件对象路径超出 Provider 根目录"));
        return Mono.fromCallable(() -> { Files.deleteIfExists(file); return (Void) null; })
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorMap(error -> error instanceof StorageUnavailableException || error instanceof ConflictException
                ? error : new StorageUnavailableException("本地附件物理删除失败: " + error.getMessage()));
    }
}
