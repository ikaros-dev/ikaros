package run.ikaros.ingestion;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;
public interface ImportRunService {
    Mono<ImportRunView> start(UUID ownerId, UUID planId, StartImportRequest request);
    Mono<List<ImportRunView>> list(UUID ownerId);
    Mono<ImportRunView> get(UUID ownerId, UUID runId);
    Mono<ImportRunView> cancel(UUID ownerId, UUID runId);
    Mono<ImportRunView> checkpoint(UUID runId, String checkpoint, long completed, long failed, long skipped);
    Mono<List<ImportRunItemView>> items(UUID ownerId, UUID runId);
    Mono<ImportRunItemView> retryItem(UUID ownerId, UUID runId, UUID itemId);
}
