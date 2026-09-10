package run.ikaros.ingestion;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface MetadataSyncSourceService {
    Mono<MetadataSyncSourceView> create(UUID ownerId, CreateMetadataSyncSourceRequest request);
    Mono<List<MetadataSyncSourceView>> list(UUID ownerId);
    Mono<MetadataSyncSourceView> enable(UUID ownerId, UUID sourceId);
    Mono<MetadataSyncSourceView> disable(UUID ownerId, UUID sourceId);
}
