package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;

public interface EpisodeCollectionRepository
    extends R2dbcRepository<EpisodeCollectionEntity, Long> {

    Mono<EpisodeCollectionEntity> findByUserIdAndEpisodeId(Long userId, Long episodeId);

}
