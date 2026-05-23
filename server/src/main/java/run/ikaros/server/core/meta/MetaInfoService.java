package run.ikaros.server.core.meta;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectRecord;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

/**
 * Meta information query service.
 */
public interface MetaInfoService {

    /**
     * Search subjects by platform and keyword.
     */
    Flux<SubjectRecord> searchSubjects(SubjectSyncPlatform platform, String keyword);

    /**
     * Get subject record by platform and platform id.
     */
    Mono<SubjectRecord> getSubjectByPlatformId(SubjectSyncPlatform platform, String platformId);
}
