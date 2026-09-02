package run.ikaros.offline;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface DownloadService {
    Mono<DownloadView> create(UUID userId, CreateDownloadRequest request);
    Flux<DownloadView> list(UUID userId, UUID deviceId);
    Mono<DownloadView> updateState(UUID userId, UUID intentId, UpdateDownloadStateRequest request);
    Mono<DownloadView> remove(UUID userId, UUID intentId);
}
