package run.ikaros.api.core.subject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
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


    Mono<Subject> syncByPlatform(@Nullable Long subjectId, SubjectSyncPlatform platform,
                                 String platformId);


    Mono<Subject> findBySubjectIdAndPlatformAndPlatformId(@Nonnull Long subjectId,
                                                          @Nonnull
                                                          SubjectSyncPlatform subjectSyncPlatform,
                                                          @NotBlank String platformId);

    Flux<Subject> findByPlatformAndPlatformId(@Nonnull SubjectSyncPlatform subjectSyncPlatform,
                                              @NotBlank String platformId);

    Mono<Boolean> existsByPlatformAndPlatformId(@Nonnull SubjectSyncPlatform subjectSyncPlatform,
                                                @NotBlank String platformId);
}
