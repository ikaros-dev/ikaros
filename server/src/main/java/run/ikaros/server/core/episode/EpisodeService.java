package run.ikaros.server.core.episode;

import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.store.enums.EpisodeGroup;

public interface EpisodeService {
    @Transactional
    Mono<Episode> save(Episode episode);

    Mono<Episode> findById(Long episodeId);

    Flux<Episode> findAllBySubjectId(Long subjectId);

    Mono<Episode> findBySubjectIdAndGroupAndSequence(Long subjectId, EpisodeGroup group,
                                                           Float sequence);

    Mono<Void> deleteById(Long episodeId);

    Mono<Long> countBySubjectId(Long subjectId);

    /**
     * 当前条目已经绑定附件的剧集数量.
     */
    Mono<Long> countMatchingBySubjectId(Long subjectId);

    Flux<EpisodeResource> findResourcesById(Long episodeId);
}
