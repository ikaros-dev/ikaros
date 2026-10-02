package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DiscoveredItemRepository extends ReactiveCrudRepository<DiscoveredItemEntity, UUID> {
    Flux<DiscoveredItemEntity> findAllByScanRunIdOrderByRelativeKeyAsc(UUID scanRunId);
    Mono<DiscoveredItemEntity> findByScanRunIdAndRelativeKey(UUID scanRunId, String relativeKey);
}
