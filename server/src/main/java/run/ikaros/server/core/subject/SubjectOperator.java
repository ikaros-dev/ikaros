package run.ikaros.server.core.subject;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectOperate;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subject.service.SubjectSyncService;

@Slf4j
@Component
public class SubjectOperator implements SubjectOperate {
    private final SubjectService subjectService;
    private final SubjectSyncService syncPlatformService;

    public SubjectOperator(SubjectService subjectService,
                           SubjectSyncService syncPlatformService) {
        this.subjectService = subjectService;
        this.syncPlatformService = syncPlatformService;
    }


    @Override
    public Mono<Subject> findById(UUID id) {
        return subjectService.findById(id);
    }

    @Override
    public Flux<Subject> findAllByPageable(PagingWrap<Subject> pagingWrap) {
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
    public Mono<Void> syncByPlatform(@Nullable UUID subjectId, SubjectSyncPlatform platform,
                                        String platformId) {
        return syncPlatformService.sync(subjectId, platform, platformId);
    }

    @Override
    public Mono<Subject> findBySubjectIdAndPlatformAndPlatformId(@NonNull UUID subjectId,
                                                                 @NonNull SubjectSyncPlatform
                                                                     subjectSyncPlatform,
                                                                 String platformId) {
        return subjectService.findBySubjectIdAndPlatformAndPlatformId(
            subjectId, subjectSyncPlatform, platformId);
    }

    @Override
    public Flux<Subject> findByPlatformAndPlatformId(
        @NonNull SubjectSyncPlatform subjectSyncPlatform, String platformId) {
        return subjectService.findByPlatformAndPlatformId(subjectSyncPlatform, platformId);
    }

    @Override
    public Mono<Boolean> existsByPlatformAndPlatformId(
        @NonNull SubjectSyncPlatform subjectSyncPlatform, String platformId) {
        return subjectService.existsByPlatformAndPlatformId(subjectSyncPlatform, platformId);
    }

}
