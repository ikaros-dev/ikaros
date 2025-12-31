package run.ikaros.server.core.subject.service;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.vo.FindSubjectCondition;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectService {
    Mono<Subject> findById(UUID id);

    Mono<Subject> findByBgmId(UUID subjectId, String bgmtvId);

    Mono<Subject> findBySubjectIdAndPlatformAndPlatformId(UUID subjectId,
                                                          @Nonnull
                                                          SubjectSyncPlatform subjectSyncPlatform,
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
    Mono<Void> deleteById(UUID id);

    Mono<PagingWrap<Subject>> findAllByPageable(PagingWrap<Subject> pagingWrap);

    Mono<PagingWrap<Subject>> listEntitiesByCondition(FindSubjectCondition condition);

    Mono<Void> deleteAll();
}
