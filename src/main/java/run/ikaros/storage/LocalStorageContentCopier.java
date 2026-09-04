package run.ikaros.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.common.ConflictException;
import run.ikaros.common.StorageUnavailableException;

@Component
public class LocalStorageContentCopier implements StorageContentCopier {
    @Override public boolean supports(StorageProvider provider) {
        return "LOCAL_FILESYSTEM".equalsIgnoreCase(provider.providerType());
    }

    @Override public Mono<String> copy(StorageProvider provider, BlobPlacementEntity source, BlobEntity blob,
                                       StorageTier targetTier) {
        Object rootValue = provider.metadata().get("rootPath");
        if (!(rootValue instanceof String rootText) || rootText.isBlank())
            return Mono.error(new StorageUnavailableException("本地 Storage Provider 缺少 rootPath 配置"));
        Path root = Path.of(rootText).toAbsolutePath().normalize();
        Path input = root.resolve(source.objectKey()).normalize();
        if (!input.startsWith(root)) return Mono.error(new ConflictException("附件对象路径超出 Provider 根目录"));
        String targetKey = source.objectKey() + ".promotion-" + UUID.randomUUID();
        Path output = root.resolve(targetKey).normalize();
        if (!output.startsWith(root)) return Mono.error(new ConflictException("Promotion 目标路径超出 Provider 根目录"));
        return Mono.fromCallable(() -> {
            if (!Files.isRegularFile(input)) throw new StorageUnavailableException("Promotion 源对象不可读");
            Files.createDirectories(output.getParent());
            Files.copy(input, output, StandardCopyOption.COPY_ATTRIBUTES);
            if (Files.size(output) != blob.sizeBytes()) {
                Files.deleteIfExists(output);
                throw new StorageUnavailableException("Promotion 对象大小校验失败");
            }
            return targetKey;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
