package run.ikaros.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class LocalStorageProviderLifecycleInspector implements StorageProviderLifecycleInspector {
    @Override public boolean supports(StorageProvider provider) {
        return "LOCAL_FILESYSTEM".equalsIgnoreCase(provider.providerType());
    }

    @Override public Mono<StorageProviderLifecycleState> inspect(StorageProvider provider, BlobPlacementEntity placement,
        BlobEntity blob) {
        Object rootValue = provider.metadata().get("rootPath");
        if (!(rootValue instanceof String rootText) || rootText.isBlank()) return Mono.just(StorageProviderLifecycleState.UNKNOWN);
        Path root = Path.of(rootText).toAbsolutePath().normalize();
        Path file = root.resolve(placement.objectKey()).normalize();
        if (!file.startsWith(root)) return Mono.just(StorageProviderLifecycleState.UNKNOWN);
        return Mono.fromCallable(() -> Files.isRegularFile(file)
            ? StorageProviderLifecycleState.PRESENT : StorageProviderLifecycleState.MISSING)
            .subscribeOn(Schedulers.boundedElastic());
    }
}
