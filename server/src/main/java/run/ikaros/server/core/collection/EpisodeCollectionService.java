package run.ikaros.server.core.collection;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.EpisodeCollection;

public interface EpisodeCollectionService {

    Mono<EpisodeCollection> create(UUID userId, UUID episodeId);

    Mono<EpisodeCollection> remove(UUID userId, UUID episodeId);

    Mono<EpisodeCollection> findByUserIdAndEpisodeId(UUID userId, UUID episodeId);

    Flux<EpisodeCollection> findAllByUserIdAndSubjectId(UUID userId, UUID subjectId);

    Mono<Void> updateEpisodeCollectionProgress(UUID userId, UUID episodeId,
                                               Long progress);

    Mono<Void> updateEpisodeCollection(UUID userId, UUID episodeId,
                                       Long progress, Long duration);

    Mono<Void> updateEpisodeCollectionFinish(UUID userId, UUID episodeId,
                                             Boolean finish);
}
