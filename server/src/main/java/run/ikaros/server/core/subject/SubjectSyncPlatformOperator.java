package run.ikaros.server.core.subject;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.core.subject.SubjectSyncPlatformOperate;
import run.ikaros.api.core.subject.vo.PostSubjectSyncCondition;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.service.SubjectSyncPlatformService;

@Slf4j
@Component
public class SubjectSyncPlatformOperator implements SubjectSyncPlatformOperate {
    private final SubjectSyncPlatformService service;

    public SubjectSyncPlatformOperator(SubjectSyncPlatformService service) {
        this.service = service;
    }

    @Override
    public Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform,
                              String platformId) {
        return service.sync(subjectId, platform, platformId);
    }

    @Override
    public Mono<Subject> sync(PostSubjectSyncCondition condition) {
        return service.sync(condition);
    }

    @Override
    public Mono<SubjectSync> save(SubjectSync subjectSync) {
        return service.save(subjectSync);
    }

    @Override
    public Mono<Void> remove(SubjectSync subjectSync) {
        return service.remove(subjectSync);
    }

    @Override
    public Flux<SubjectSync> findSubjectSyncsBySubjectId(long subjectId) {
        return service.findSubjectSyncsBySubjectId(subjectId);
    }

    @Override
    public Mono<SubjectSync> findSubjectSyncBySubjectIdAndPlatform(long subjectId,
                                                                   SubjectSyncPlatform platform) {
        return service.findSubjectSyncBySubjectIdAndPlatform(subjectId, platform);
    }

    @Override
    public Flux<SubjectSync> findSubjectSyncsByPlatformAndPlatformId(SubjectSyncPlatform platform,
                                                                     String platformId) {
        return service.findSubjectSyncsByPlatformAndPlatformId(platform, platformId);
    }

    @Override
    public Mono<SubjectSync> findBySubjectIdAndPlatformAndPlatformId(Long subjectId,
                                                                     SubjectSyncPlatform platform,
                                                                     String platformId) {
        return service.findBySubjectIdAndPlatformAndPlatformId(subjectId, platform, platformId);
    }
}
