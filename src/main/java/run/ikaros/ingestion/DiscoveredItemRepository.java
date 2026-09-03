package run.ikaros.ingestion;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DiscoveredItemRepository extends ReactiveCrudRepository<DiscoveredItemEntity, UUID> {
    Flux<DiscoveredItemEntity> findAllByScanRunIdOrderByRelativeKeyAsc(UUID scanRunId);
}
