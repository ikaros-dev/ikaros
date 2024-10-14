package run.ikaros.server.core.subject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectMeta;
import run.ikaros.api.core.subject.SubjectOperate;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subject.service.SubjectSyncPlatformService;

@Slf4j
@Component
public class SubjectOperator implements SubjectOperate {
    private final SubjectService subjectService;
    private final SubjectSyncPlatformService syncPlatformService;

    public SubjectOperator(SubjectService subjectService,
                           SubjectSyncPlatformService syncPlatformService) {
        this.subjectService = subjectService;
        this.syncPlatformService = syncPlatformService;
    }


    @Override
    public Mono<Subject> findById(Long id) {
        return subjectService.findById(id);
    }

    @Override
    public Flux<SubjectMeta> findAllByPageable(PagingWrap<Subject> pagingWrap) {
        return subjectService.findAllByPageable(pagingWrap)
            .map(PagingWrap::getItems)
            .flatMapMany(subjects -> Flux.fromStream(subjects.stream()));
    }


    @Override
    public Mono<Subject> create(Subject subject) {
        return subjectService.create(subject);
    }

    @Override
    public Mono<Void> update(Subject subject) {
        return subjectService.update(subject);
    }

    @Override
    public Mono<Subject> syncByPlatform(@Nullable Long subjectId, SubjectSyncPlatform platform,
                                        String platformId) {
        return syncPlatformService.sync(subjectId, platform, platformId);
    }

    @Override
    public Mono<Subject> findBySubjectIdAndPlatformAndPlatformId(@Nonnull Long subjectId,
                                                                 @Nonnull SubjectSyncPlatform
                                                                     subjectSyncPlatform,
                                                                 String platformId) {
        return subjectService.findBySubjectIdAndPlatformAndPlatformId(
            subjectId, subjectSyncPlatform, platformId);
    }

    @Override
    public Flux<Subject> findByPlatformAndPlatformId(
        @Nonnull SubjectSyncPlatform subjectSyncPlatform, String platformId) {
        return subjectService.findByPlatformAndPlatformId(subjectSyncPlatform, platformId);
    }

    @Override
    public Mono<Boolean> existsByPlatformAndPlatformId(
        @Nonnull SubjectSyncPlatform subjectSyncPlatform, String platformId) {
        return subjectService.existsByPlatformAndPlatformId(subjectSyncPlatform, platformId);
    }

}
