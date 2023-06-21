package run.ikaros.api.core.subject;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectOperate extends AllowPluginOperate {

    Mono<Subject> findById(Long id);

    Flux<Subject> findAllByPageable(PagingWrap<Subject> pagingWrap);

    Mono<Subject> create(Subject subject);

    Mono<Void> update(Subject subject);

    Mono<Void> removeById(Long id);
}
