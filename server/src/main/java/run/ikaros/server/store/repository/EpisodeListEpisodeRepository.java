package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.EpisodeListEpisodeEntity;

public interface EpisodeListEpisodeRepository
    extends BaseRepository<EpisodeListEpisodeEntity> {

    Flux<EpisodeListEpisodeEntity> findAllByEpisodeListId(UUID episodeListId);

    Mono<Void> deleteByEpisodeListId(UUID episodeListId);
}
