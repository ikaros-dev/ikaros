package run.ikaros.server.core.subject;

import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

public interface SubjectSyncPlatformService {
    Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform, String platformId);
}
