package run.ikaros.server.core.subject.service;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectMeta;
import run.ikaros.api.core.subject.vo.FindSubjectCondition;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectService {
    Mono<Subject> findById(Long id);

    Mono<Subject> findByBgmId(@Nonnull Long subjectId, Long bgmtvId);

    Mono<Subject> findBySubjectIdAndPlatformAndPlatformId(@Nonnull Long subjectId,
                                     @Nonnull SubjectSyncPlatform subjectSyncPlatform,
                                     @NotBlank String platformId);

    Flux<Subject> findByPlatformAndPlatformId(@Nonnull SubjectSyncPlatform subjectSyncPlatform,
                                              @NotBlank String platformId);

    Mono<Boolean> existsByPlatformAndPlatformId(@Nonnull SubjectSyncPlatform subjectSyncPlatform,
                                                @NotBlank String platformId);

    @Transactional
    Mono<Subject> create(Subject subject);

    @Transactional
    Mono<Void> update(Subject subject);

    @Transactional
    Mono<Void> deleteById(Long id);

    Mono<PagingWrap<SubjectMeta>> findAllByPageable(PagingWrap<Subject> pagingWrap);

    Mono<PagingWrap<SubjectMeta>> listEntitiesByCondition(FindSubjectCondition condition);

    Mono<Void> deleteAll();
}
