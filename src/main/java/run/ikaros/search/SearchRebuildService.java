package run.ikaros.search;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SearchRebuildService {
    Mono<SearchRebuildResult> rebuild(Flux<SearchProjectionInput> source, String projectorVersion);
}
