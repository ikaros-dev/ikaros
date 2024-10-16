package run.ikaros.api.core.subject;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.EpisodeGroup;

public interface EpisodeOperate extends AllowPluginOperate {
    Mono<Episode> save(Episode episode);

    Mono<Episode> findById(Long episodeId);

    Flux<Episode> findAllBySubjectId(Long subjectId);

    Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        Long subjectId, EpisodeGroup group, Float sequence, String name);

    Flux<Episode> findBySubjectIdAndGroupAndSequence(Long subjectId, EpisodeGroup group,
                                                     Float sequence);

    Mono<Long> countBySubjectId(Long subjectId);

    /**
     * 当前条目已经绑定附件的剧集数量.
     */
    Mono<Long> countMatchingBySubjectId(Long subjectId);

    Flux<EpisodeResource> findResourcesById(Long episodeId);
}
