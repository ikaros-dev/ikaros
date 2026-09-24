package run.ikaros.ingestion;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MetadataSyncService {
    Mono<MetadataRefreshResult> detect(UUID ownerId, UUID syncSourceId, DetectMetadataUpdateRequest request);
    Flux<MetadataSyncStatusView> status(UUID ownerId, UUID syncSourceId);
}
