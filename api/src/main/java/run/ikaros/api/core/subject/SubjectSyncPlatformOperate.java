package run.ikaros.api.core.subject;

import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.vo.PostSubjectSyncCondition;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

public interface SubjectSyncPlatformOperate extends AllowPluginOperate {
    Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform, String platformId);

    Mono<Subject> sync(PostSubjectSyncCondition condition);

    Mono<SubjectSync> save(SubjectSync subjectSync);

    Flux<SubjectSync> findSubjectSyncsBySubjectId(long subjectId);

    Mono<SubjectSync> findSubjectSyncBySubjectIdAndPlatform(long subjectId,
                                                            SubjectSyncPlatform platform);

    Flux<SubjectSync> findSubjectSyncsByPlatformAndPlatformId(SubjectSyncPlatform platform,
                                                              String platformId);

    Mono<SubjectSync> findBySubjectIdAndPlatformAndPlatformId(Long subjectId,
                                                              SubjectSyncPlatform platform,
                                                              String platformId);
}
