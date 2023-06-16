package run.ikaros.server.core.subject;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.wrap.PagingWrap;

public interface SubjectService {
    Mono<Subject> findById(Long id);

    Mono<Subject> findByBgmId(Long bgmtvId);

    Mono<Subject> findBySyncPlatform(@Nonnull SubjectSyncPlatform subjectSyncPlatform,
                                     @NotBlank String platformId);

    @Transactional
    Mono<Subject> create(Subject subject);

    @Transactional
    Mono<Void> update(Subject subject);

    @Transactional
    Mono<Void> deleteById(Long id);

    Mono<PagingWrap<Subject>> findAllByPageable(PagingWrap<Subject> pagingWrap);

    Mono<PagingWrap<Subject>> listEntitiesByCondition(FindSubjectCondition condition);

    Mono<Void> deleteAll();
}
