package run.ikaros.server.core.meta;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;

/**
 * Meta service.
 */
public interface MetaService {
    Mono<Subject> findOne(String keyword);

    Flux<Subject> findAll(String keyword);
}
