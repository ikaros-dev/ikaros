package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;

public interface EpisodeCollectionRepository
    extends BaseRepository<EpisodeCollectionEntity> {

    Mono<EpisodeCollectionEntity> findByUserIdAndEpisodeId(UUID userId, UUID episodeId);

    Flux<EpisodeCollectionEntity> findAllByUserIdAndSubjectId(UUID userId, UUID subjectId);

}
