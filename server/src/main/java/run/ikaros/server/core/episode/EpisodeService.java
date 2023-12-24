package run.ikaros.server.core.episode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeMeta;
import run.ikaros.api.core.subject.EpisodeResource;

public interface EpisodeService {
    Mono<EpisodeMeta> findMetaById(Long episodeId);

    Mono<Episode> findById(Long episodeId);

    Flux<EpisodeResource> findResourcesById(Long episodeId);
}
