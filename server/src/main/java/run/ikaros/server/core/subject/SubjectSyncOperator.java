package run.ikaros.server.core.subject;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.core.subject.SubjectSyncOperate;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.service.SubjectSyncService;

@Slf4j
@Component
public class SubjectSyncOperator implements SubjectSyncOperate {
    private final SubjectSyncService service;

    public SubjectSyncOperator(SubjectSyncService service) {
        this.service = service;
    }


    @Override
    public Mono<Void> sync(@Nullable Long subjectId, SubjectSyncPlatform platform,
                           String platformId) {
        return service.sync(subjectId, platform, platformId);
    }

    @Override
    public Mono<SubjectSync> save(SubjectSync subjectSync) {
        return service.save(subjectSync);
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
