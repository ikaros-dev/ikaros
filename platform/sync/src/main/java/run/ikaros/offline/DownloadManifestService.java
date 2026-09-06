package run.ikaros.offline;
import java.util.UUID;
import reactor.core.publisher.Mono;
public interface DownloadManifestService {
    Mono<DownloadManifestView> create(UUID userId, UUID intentId, CreateDownloadManifestRequest request);
    Mono<DownloadManifestView> get(UUID userId, UUID intentId);
}
