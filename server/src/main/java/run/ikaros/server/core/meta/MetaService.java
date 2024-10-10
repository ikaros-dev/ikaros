package run.ikaros.server.core.meta;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectMeta;

/**
 * Meta service.
 */
public interface MetaService {
    Mono<SubjectMeta> findOne(String keyword);

    Flux<SubjectMeta> findAll(String keyword);
}
