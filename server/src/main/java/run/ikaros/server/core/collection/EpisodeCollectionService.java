package run.ikaros.server.core.collection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.EpisodeCollection;

public interface EpisodeCollectionService {

    Mono<EpisodeCollection> create(Long userId, Long episodeId);

    Mono<EpisodeCollection> remove(Long userId, Long episodeId);

    Mono<EpisodeCollection> findByUserIdAndEpisodeId(Long userId, Long episodeId);

    Flux<EpisodeCollection> findAllByUserIdAndSubjectId(Long userId, Long subjectId);

    Mono<Void> updateEpisodeCollectionProgress(Long userId, Long episodeId,
                                               Long progress);

    Mono<Void> updateEpisodeCollection(Long userId, Long episodeId,
                                       Long progress, Long duration);

    Mono<Void> updateEpisodeCollectionFinish(Long userId, Long episodeId,
                                             Boolean finish);
}
