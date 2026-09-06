package run.ikaros.storage;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LocalStorageRestoreStatusQuery implements StorageRestoreStatusQuery {
    private final StorageRestoreExecutor executor;
    public LocalStorageRestoreStatusQuery(StorageRestoreExecutor executor) { this.executor = executor; }
    @Override public boolean supports(StorageProvider provider) { return "LOCAL_FILESYSTEM".equalsIgnoreCase(provider.providerType()); }
    @Override public Mono<StorageRestoreProviderStatus> query(StorageProvider provider, BlobPlacementEntity placement, BlobEntity blob) {
        return executor.restore(provider, placement, blob).map(result -> result.readable()
            ? StorageRestoreProviderStatus.READABLE : StorageRestoreProviderStatus.NOT_READABLE);
    }
}
