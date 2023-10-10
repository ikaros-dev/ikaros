package run.ikaros.api.core.subject;

import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectOperate extends AllowPluginOperate {

    Mono<Subject> findById(Long id);

    Flux<SubjectMeta> findAllByPageable(PagingWrap<Subject> pagingWrap);

    Mono<Subject> create(Subject subject);

    Mono<Void> update(Subject subject);

    Mono<Void> removeById(Long id);

    Mono<Subject> syncByPlatform(@Nullable Long subjectId, SubjectSyncPlatform platform,
                                 String platformId);

    Mono<Subject> findByPlatform(@Nullable Long subjectId, SubjectSyncPlatform platform,
                                 String keyword);
}
