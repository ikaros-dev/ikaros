package run.ikaros.server.core.episode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;

public interface EpisodeService {
    Mono<Episode> findById(Long episodeId);

    Mono<Long> countBySubjectId(Long subjectId);

    /**
     * 当前条目已经绑定附件的剧集数量.
     */
    Mono<Long> countMatchingBySubjectId(Long subjectId);

    Flux<EpisodeResource> findResourcesById(Long episodeId);
}
