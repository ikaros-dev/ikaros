package run.ikaros.api.core.meta;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectRecord;
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
    Flux<SubjectRecord> searchSubjects(String keyword);

    /**
     * Get a single subject record by platform id.
     */
    Mono<SubjectRecord> getSubjectByPlatformId(String platformId);
}
