package run.ikaros.server.core.meta;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

/**
 * Meta information query service.
 */
public interface MetaInfoService {

    /**
     * Search subjects by platform and keyword.
     */
    Flux<Subject> searchSubjects(SubjectSyncPlatform platform, String keyword);

    /**
     * Get subject by platform and platform id.
     */
    Mono<Subject> getSubjectByPlatformId(SubjectSyncPlatform platform, String platformId);

    /**
     * Get episodes by platform and platform id.
     */
    Flux<Episode> getEpisodesByPlatformId(SubjectSyncPlatform platform, String platformId);

    /**
     * Get tag names by platform and platform id.
     */
    Flux<String> getTagsByPlatformId(SubjectSyncPlatform platform, String platformId);
}
