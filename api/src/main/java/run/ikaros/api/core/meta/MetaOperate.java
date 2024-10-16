package run.ikaros.api.core.meta;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.plugin.AllowPluginOperate;

public interface MetaOperate extends AllowPluginOperate {
    Mono<Subject> findOne(String keyword);

    Flux<Subject> findAll(String keyword);
}
