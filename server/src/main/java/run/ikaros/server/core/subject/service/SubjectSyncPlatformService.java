package run.ikaros.server.core.subject.service;

import jakarta.annotation.Nullable;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.core.subject.vo.PostSubjectSyncCondition;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

public interface SubjectSyncPlatformService {
    Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform, String platformId);

    Mono<Subject> sync(PostSubjectSyncCondition condition);

    /**
     * 同步对应平台ID条目及其所有的相关条目.
     *
     * @param platform   平台
     * @param platformId 平台ID
     * @return 第一个(起点)条目
     */
    Mono<Subject> syncSelfAndRelations(SubjectSyncPlatform platform, String platformId);

    @Transactional
    Mono<SubjectSync> save(SubjectSync subjectSync);

    @Transactional
    Mono<Void> remove(SubjectSync subjectSync);

    Flux<SubjectSync> findSubjectSyncsBySubjectId(long subjectId);

    Mono<SubjectSync> findSubjectSyncBySubjectIdAndPlatform(long subjectId,
                                                            SubjectSyncPlatform platform);

    Flux<SubjectSync> findSubjectSyncsByPlatformAndPlatformId(SubjectSyncPlatform platform,
                                                              String platformId);

    Mono<SubjectSync> findBySubjectIdAndPlatformAndPlatformId(Long subjectId,
                                                              SubjectSyncPlatform platform,
                                                              String platformId);
}
