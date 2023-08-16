package run.ikaros.server.core.collection;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.EpisodeCollection;

public interface EpisodeCollectionService {

    Mono<EpisodeCollection> create(Long userId, Long episodeId);

    Mono<EpisodeCollection> remove(Long userId, Long episodeId);

    Mono<EpisodeCollection> findByUserIdAndEpisodeId(Long userId, Long episodeId);

    Mono<Void> updateEpisodeCollectionProgress(Long userId, Long episodeId,
                                               Long progress);

    Mono<Void> updateEpisodeCollectionFinish(Long userId, Long episodeId,
                                             Boolean finish);
}
