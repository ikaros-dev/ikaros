package run.ikaros.ingestion;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DiscoveredItemService {
    Mono<DiscoveredItemView> record(UUID ownerId, UUID scanRunId, DiscoveredItemRequest request);
    Mono<List<DiscoveredItemView>> list(UUID ownerId, UUID scanRunId);
    Mono<DiscoveredItemView> markUnavailable(UUID ownerId, UUID itemId, String reason);
}
