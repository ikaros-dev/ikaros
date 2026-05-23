package run.ikaros.api.core.meta;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.plugin.IkarosExtensionPoint;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

/**
 * Meta information query extension point for fetching entries from third-party metadata websites.
 * Plugins implement this interface to provide metadata query capability for a specific platform.
 */
public interface MetaInfoExtensionPoint extends IkarosExtensionPoint {

    /**
     * The platform this extension point handles, e.g. BGM_TV for Bangumi plugin.
     *
     * @see SubjectSyncPlatform
     */
    SubjectSyncPlatform getPlatform();

    /**
     * Search subjects by keyword, returning the most matching results.
     */
    Flux<Subject> searchSubjects(String keyword);

    /**
     * Get a single subject by platform id.
     */
    Mono<Subject> getSubjectByPlatformId(String platformId);

    /**
     * Get episodes for a subject by platform id.
     */
    Flux<Episode> getEpisodesByPlatformId(String platformId);

    /**
     * Get tag names for a subject by platform id.
     */
    Flux<String> getTagsByPlatformId(String platformId);
}
