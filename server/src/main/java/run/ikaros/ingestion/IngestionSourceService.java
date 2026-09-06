package run.ikaros.ingestion;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface IngestionSourceService {
    Mono<IngestionSourceView> create(UUID ownerId, CreateIngestionSourceRequest request);
    Mono<List<IngestionSourceView>> list(UUID ownerId);
    Mono<IngestionSourceView> get(UUID ownerId, UUID sourceId);
    Mono<IngestionSourceView> enable(UUID ownerId, UUID sourceId);
    Mono<IngestionSourceView> disable(UUID ownerId, UUID sourceId);
}
