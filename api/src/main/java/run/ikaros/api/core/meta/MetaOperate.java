package run.ikaros.api.core.meta;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectMeta;
import run.ikaros.api.plugin.AllowPluginOperate;

public interface MetaOperate extends AllowPluginOperate {
    Mono<SubjectMeta> findOne(String keyword);

    Flux<SubjectMeta> findAll(String keyword);
}
