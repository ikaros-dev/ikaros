package run.ikaros.server.core.subject.service;

import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.core.subject.vo.PostSubjectSyncCondition;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.store.entity.SubjectSyncEntity;

public interface SubjectSyncPlatformService {
    Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform, String platformId);

    Mono<Subject> sync(PostSubjectSyncCondition condition);

    Mono<SubjectSync> save(SubjectSync subjectSync);

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
