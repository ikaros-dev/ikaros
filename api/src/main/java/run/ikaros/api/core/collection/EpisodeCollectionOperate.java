package run.ikaros.api.core.collection;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface EpisodeCollectionOperate extends CollectionOperate {
    Mono<EpisodeCollection> create(UUID userId, UUID episodeId);

    Mono<EpisodeCollection> findByUserIdAndEpisodeId(UUID userId, UUID episodeId);

    Mono<Void> updateEpisodeCollectionProgress(UUID userId, UUID episodeId,
                                               Long progress);

    Mono<Void> updateEpisodeCollection(UUID userId, UUID episodeId,
                                       Long progress, Long duration);

    Mono<Void> updateEpisodeCollectionFinish(UUID userId, UUID episodeId,
                                             Boolean finish);
}
