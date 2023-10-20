package run.ikaros.server.search;

import reactor.core.publisher.Mono;

public interface IndicesService {

    Mono<Void> rebuildSubjectIndices();

}
