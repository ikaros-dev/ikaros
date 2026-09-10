package run.ikaros.search;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface SearchQueryService {
    Mono<SearchPage> search(UUID actorId, SearchQueryRequest request);
}
