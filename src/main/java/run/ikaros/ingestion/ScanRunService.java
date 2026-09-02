package run.ikaros.ingestion;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface ScanRunService {
    Mono<ScanRunView> start(UUID ownerId, UUID sourceId, StartScanRequest request);
    Mono<List<ScanRunView>> list(UUID ownerId);
    Mono<ScanRunView> get(UUID ownerId, UUID scanId);
    Mono<ScanRunView> cancel(UUID ownerId, UUID scanId);
    Mono<ScanRunView> checkpoint(UUID scanId, String checkpoint, long discovered, long changed,
                                 long skipped, String errorSummary);
}
