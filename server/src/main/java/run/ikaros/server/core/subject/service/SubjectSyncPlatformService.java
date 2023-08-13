package run.ikaros.server.core.subject.service;

import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.vo.PostSubjectSyncCondition;

public interface SubjectSyncPlatformService {
    Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform, String platformId);

    Mono<Subject> sync(PostSubjectSyncCondition condition);
}
