package run.ikaros.server.search;

import reactor.core.publisher.Mono;

public interface IndicesService {

    Mono<Void> rebuildFileIndices();

    Mono<Void> rebuildSubjectIndices();

}
